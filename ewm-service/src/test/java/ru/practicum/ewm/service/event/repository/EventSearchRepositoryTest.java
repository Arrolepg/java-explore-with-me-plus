package ru.practicum.ewm.service.event.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.ewm.service.category.model.Category;
import ru.practicum.ewm.service.config.JpaAuditingConfig;
import ru.practicum.ewm.service.event.model.Event;
import ru.practicum.ewm.service.event.model.EventLocation;
import ru.practicum.ewm.service.event.model.EventState;
import ru.practicum.ewm.service.event.utility.AdminEventSearchFilter;
import ru.practicum.ewm.service.event.utility.PublicEventSearchFilter;
import ru.practicum.ewm.service.request.model.Request;
import ru.practicum.ewm.service.request.model.RequestStatus;
import ru.practicum.ewm.service.request.repository.RequestRepository;
import ru.practicum.ewm.service.user.model.User;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
public class EventSearchRepositoryTest {
    private static final Specification<Event> ANY_EVENT = Specification.where(null);
    private static final LocalDateTime BASE_DATE = LocalDateTime.now().plusDays(30);

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private RequestRepository requestRepository;

    private Event first;
    private Event second;
    private Event third;
    private User owner;
    private User otherUser;
    private User thirdUser;

    @BeforeEach
    void setUp() {
        owner = createUser("Owner", "owner@mail.com");
        otherUser = createUser("Other", "other@mail.com");
        thirdUser = createUser("Third", "third@mail.com");

        Category category = createCategory();
        first = createEvent(owner, category, LocalDateTime.now().plusDays(3));
        second = createEvent(owner, category, LocalDateTime.now().plusDays(1));
        third = createEvent(owner, category, LocalDateTime.now().plusDays(2));
        entityManager.clear();
    }

    @Test
    void testSortAscByIdFromPageable() {
        List<Event> result = eventRepository.findAllWithFetch(ANY_EVENT,
                PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "id")));

        assertThat(result).extracting(Event::getId)
                .containsExactly(first.getId(), second.getId(), third.getId());
    }

    @Test
    void testSortDescByEventDateFromPageable() {
        List<Event> result = eventRepository.findAllWithFetch(ANY_EVENT,
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "eventDate")));

        assertThat(result).extracting(Event::getId)
                .containsExactly(first.getId(), third.getId(), second.getId());
    }

    @Test
    void testOffsetAndSizeFromPageable() {
        List<Event> result = eventRepository.findAllWithFetch(ANY_EVENT,
                PageRequest.of(1, 2, Sort.by(Sort.Direction.ASC, "id")));

        assertThat(result).extracting(Event::getId).containsExactly(third.getId());
    }

    @Test
    void testSpecificationIsApplied() {
        AdminEventSearchFilter request = new AdminEventSearchFilter(List.of(owner.getId()),
                List.of(EventState.PUBLISHED), null, null, null);

        List<Event> result = eventRepository.findAllWithFetch(EventSpecifications.forAdminSearch(request),
                PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "id")));

        assertThat(result).isEmpty();
    }

    @Test
    void testCategoryAndInitiatorAreFetched() {
        List<Event> result = eventRepository.findAllWithFetch(ANY_EVENT,
                PageRequest.of(0, 1, Sort.by(Sort.Direction.ASC, "id")));
        entityManager.clear();

        assertAll(
                () -> assertThat(result.getFirst().getCategory().getName()).isEqualTo("Category"),
                () -> assertThat(result.getFirst().getInitiator().getName()).isEqualTo("Owner")
        );
    }

    @Test
    void testPublicSearchOnlyPublished() {
        Event published = createPublishedEvent(owner, createCategory("Concert"),
                BASE_DATE.plusDays(5), "Опубликованный концерт", false, 10);
        entityManager.flush();
        entityManager.clear();

        PublicEventSearchFilter filter = new PublicEventSearchFilter(
                null, null, null, null, null, false);

        List<Event> result = eventRepository.findAllWithFetch(
                EventSpecifications.forPublicSearch(filter),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "id")));

        assertThat(result).extracting(Event::getId).containsExactly(published.getId());
    }

    @Test
    void testPublicSearchByTextMatchesAnnotation() {
        Event published = createPublishedEvent(owner, createCategory("Concert"),
                BASE_DATE.plusDays(5), "Уникальный текст события", false, 10);
        entityManager.flush();
        entityManager.clear();

        PublicEventSearchFilter filter = new PublicEventSearchFilter(
                "Уникальный", null, null, null, null, false);

        List<Event> result = eventRepository.findAllWithFetch(
                EventSpecifications.forPublicSearch(filter),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "id")));

        assertThat(result).extracting(Event::getId).containsExactly(published.getId());
    }

    @Test
    void testPublicSearchByTextCaseInsensitive() {
        Event published = createPublishedEvent(owner, createCategory("Concert"),
                BASE_DATE.plusDays(5), "Уникальный текст", false, 10);
        entityManager.flush();
        entityManager.clear();

        PublicEventSearchFilter filter = new PublicEventSearchFilter(
                "УНИКАЛЬНЫЙ", null, null, null, null, false);

        List<Event> result = eventRepository.findAllWithFetch(
                EventSpecifications.forPublicSearch(filter),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "id")));

        assertThat(result).extracting(Event::getId).containsExactly(published.getId());
    }

    @Test
    void testPublicSearchByCategory() {
        Category concert = createCategory("Concert");

        Event published = createPublishedEvent(owner, concert,
                BASE_DATE.plusDays(5), "Концерт", false, 10);
        entityManager.flush();
        entityManager.clear();

        PublicEventSearchFilter filter = new PublicEventSearchFilter(
                null, List.of(concert.getId()), null, null, null, false);

        List<Event> result = eventRepository.findAllWithFetch(
                EventSpecifications.forPublicSearch(filter),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "id")));

        assertThat(result).extracting(Event::getId).containsExactly(published.getId());
    }

    @Test
    void testPublicSearchByPaid() {
        Category category = createCategory("Concert");
        Event paid = createPublishedEvent(owner, category,
                BASE_DATE.plusDays(5), "Платный концерт", true, 10);
        createPublishedEvent(owner, category,
                BASE_DATE.plusDays(6), "Бесплатный концерт", false, 10);
        entityManager.flush();
        entityManager.clear();

        PublicEventSearchFilter filter = new PublicEventSearchFilter(
                null, null, true, null, null, false);

        List<Event> result = eventRepository.findAllWithFetch(
                EventSpecifications.forPublicSearch(filter),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "id")));

        assertThat(result).extracting(Event::getId).containsExactly(paid.getId());
    }

    @Test
    void testPublicSearchByDateRange() {
        Category category = createCategory("Concert");
        Event inRange = createPublishedEvent(owner, category,
                BASE_DATE.plusDays(5), "В диапазоне", false, 10);
        createPublishedEvent(owner, category,
                BASE_DATE.plusDays(15), "Вне диапазона", false, 10);
        entityManager.flush();
        entityManager.clear();

        PublicEventSearchFilter filter = new PublicEventSearchFilter(
                null, null, null, BASE_DATE.plusDays(4), BASE_DATE.plusDays(6), false);

        List<Event> result = eventRepository.findAllWithFetch(
                EventSpecifications.forPublicSearch(filter),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "id")));

        assertThat(result).extracting(Event::getId).containsExactly(inRange.getId());
    }

    @Test
    void testPublicSearchByDefaultDateFilterReturnOnlyFutureEvents() {
        Category category = createCategory("Concert");
        Event future = createPublishedEvent(owner, category,
                BASE_DATE.plusDays(5), "Будущее", false, 10);
        createPublishedEvent(owner, category,
                LocalDateTime.now().minusDays(5), "Прошлое", false, 10);
        entityManager.flush();
        entityManager.clear();

        PublicEventSearchFilter filter = new PublicEventSearchFilter(
                null, null, null, null, null, false);

        List<Event> result = eventRepository.findAllWithFetch(
                EventSpecifications.forPublicSearch(filter),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "id")));

        assertThat(result).extracting(Event::getId).containsExactly(future.getId());
    }

    @Test
    void testPublicSearch_onlyAvailable_excludesFullEvent() {
        Category category = createCategory("Concert");
        Event full = createPublishedEvent(owner, category,
                BASE_DATE.plusDays(5), "Полный концерт", false, 3);

        createConfirmedRequest(full, owner);
        createConfirmedRequest(full, otherUser);
        createConfirmedRequest(full, thirdUser);
        entityManager.flush();
        entityManager.clear();

        PublicEventSearchFilter filter = new PublicEventSearchFilter(
                null, null, null, null, null, true);

        List<Event> result = eventRepository.findAllWithFetch(
                EventSpecifications.forPublicSearch(filter),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "id")));

        assertThat(result).extracting(Event::getId).doesNotContain(full.getId());
    }

    @Test
    void testPublicSearch_onlyAvailable_excludesZeroLimitEvent() {
        Category category = createCategory("Concert");
        Event zeroLimit = createPublishedEvent(owner, category,
                BASE_DATE.plusDays(5), "Без лимита", false, 0);
        entityManager.flush();
        entityManager.clear();

        PublicEventSearchFilter filter = new PublicEventSearchFilter(
                null, null, null, null, null, true);

        List<Event> result = eventRepository.findAllWithFetch(
                EventSpecifications.forPublicSearch(filter),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "id")));

        assertThat(result).extracting(Event::getId).doesNotContain(zeroLimit.getId());
    }

    @Test
    void testPublicSearch_onlyAvailable_includesEventWithFreeSlots() {
        Category category = createCategory("Concert");
        Event available = createPublishedEvent(owner, category,
                BASE_DATE.plusDays(5), "Свободный концерт", false, 10);

        createConfirmedRequest(available, otherUser);
        entityManager.flush();
        entityManager.clear();

        PublicEventSearchFilter filter = new PublicEventSearchFilter(
                null, null, null, null, null, true);

        List<Event> result = eventRepository.findAllWithFetch(
                EventSpecifications.forPublicSearch(filter),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "id")));

        assertThat(result).extracting(Event::getId).contains(available.getId());
    }

    @Test
    void testPublicSearch_onlyAvailable_countsOnlyConfirmed() {
        Category category = createCategory("Concert");
        Event available = createPublishedEvent(owner, category,
                BASE_DATE.plusDays(5), "Свободный концерт", false, 2);

        createRequest(available, owner, RequestStatus.PENDING);
        createRequest(available, otherUser, RequestStatus.REJECTED);
        entityManager.flush();
        entityManager.clear();

        PublicEventSearchFilter filter = new PublicEventSearchFilter(
                null, null, null, null, null, true);

        List<Event> result = eventRepository.findAllWithFetch(
                EventSpecifications.forPublicSearch(filter),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "id")));

        assertThat(result).extracting(Event::getId).contains(available.getId());
    }

    private User createUser(String name, String email) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        return entityManager.persistAndFlush(user);
    }

    private Category createCategory() {
        return createCategory("Category");
    }

    private Category createCategory(String name) {
        Category category = new Category();
        category.setName(name);
        return entityManager.persistAndFlush(category);
    }

    private Event createEvent(User initiator, Category category, LocalDateTime eventDate) {
        Event event = Event.builder()
                .annotation("Some annotation text here for event")
                .category(category)
                .description("Some description text here for event")
                .eventDate(eventDate)
                .eventLocation(EventLocation.builder().lat(55.75f).lon(37.62f).build())
                .paid(false)
                .participantLimit(0)
                .requestModeration(true)
                .title("Title")
                .initiator(initiator)
                .build();
        return entityManager.persistAndFlush(event);
    }

    private Event createPublishedEvent(User initiator, Category category, LocalDateTime eventDate,
                                       String annotation, boolean paid, int participantLimit) {
        Event event = Event.builder()
                .annotation(annotation)
                .category(category)
                .description(annotation)
                .eventDate(eventDate)
                .eventLocation(EventLocation.builder().lat(55.75f).lon(37.62f).build())
                .paid(paid)
                .participantLimit(participantLimit)
                .requestModeration(true)
                .title("Title")
                .state(EventState.PUBLISHED)
                .publishedOn(LocalDateTime.now())
                .initiator(initiator)
                .build();
        return entityManager.persistAndFlush(event);
    }

    private void createConfirmedRequest(Event event, User requester) {
        createRequest(event, requester, RequestStatus.CONFIRMED);
    }

    private void createRequest(Event event, User requester, RequestStatus status) {
        Request request = Request.builder()
                .event(event)
                .requester(requester)
                .status(status)
                .created(LocalDateTime.now())
                .build();
        requestRepository.save(request);
    }
}