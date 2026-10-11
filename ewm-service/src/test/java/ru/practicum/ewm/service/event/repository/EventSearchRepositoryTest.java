package ru.practicum.ewm.service.event.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.ewm.service.category.model.Category;
import ru.practicum.ewm.service.event.model.Event;
import ru.practicum.ewm.service.event.model.EventLocation;
import ru.practicum.ewm.service.event.model.EventState;
import ru.practicum.ewm.service.event.utility.AdminEventSearchRequest;
import ru.practicum.ewm.service.user.model.User;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@ActiveProfiles("test")
public class EventSearchRepositoryTest {
    private static final Specification<Event> ANY_EVENT = Specification.where(null);

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private EventRepository eventRepository;

    private Event first;
    private Event second;
    private Event third;
    private User owner;

    @BeforeEach
    void setUp() {
        owner = createUser();
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
        AdminEventSearchRequest request = new AdminEventSearchRequest(List.of(owner.getId()),
                List.of(EventState.PUBLISHED), null, null, null, 0, 10);

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

    private User createUser() {
        User user = new User();
        user.setName("Owner");
        user.setEmail("owner@mail.com");
        return entityManager.persistAndFlush(user);
    }

    private Category createCategory() {
        Category category = new Category();
        category.setName("Category");
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
                .state(EventState.PENDING)
                .createdOn(LocalDateTime.now())
                .initiator(initiator)
                .build();
        return entityManager.persistAndFlush(event);
    }
}
