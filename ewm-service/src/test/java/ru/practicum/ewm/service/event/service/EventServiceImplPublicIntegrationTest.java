package ru.practicum.ewm.service.event.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.service.adapter.EventStatsAdapter;
import ru.practicum.ewm.service.category.model.Category;
import ru.practicum.ewm.service.category.repository.CategoryRepository;
import ru.practicum.ewm.service.event.dto.response.EventFullDto;
import ru.practicum.ewm.service.event.dto.response.EventShortDto;
import ru.practicum.ewm.service.event.model.Event;
import ru.practicum.ewm.service.event.model.EventLocation;
import ru.practicum.ewm.service.event.model.EventSearchSort;
import ru.practicum.ewm.service.event.model.EventState;
import ru.practicum.ewm.service.event.repository.EventRepository;
import ru.practicum.ewm.service.event.utility.EventStatsHitParams;
import ru.practicum.ewm.service.event.utility.EventPaginationParams;
import ru.practicum.ewm.service.event.utility.PublicEventSearchFilter;
import ru.practicum.ewm.service.exception.NotFoundException;
import ru.practicum.ewm.service.request.model.Request;
import ru.practicum.ewm.service.request.model.RequestStatus;
import ru.practicum.ewm.service.request.repository.RequestRepository;
import ru.practicum.ewm.service.user.model.User;
import ru.practicum.ewm.service.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EventServiceImplPublicIntegrationTest {
    private static final String URI = "/events";
    private static final String IP = "127.0.0.1";
    private static final LocalDateTime BASE_DATE = LocalDateTime.now().plusDays(30);

    @Autowired
    private EventService eventService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private RequestRepository requestRepository;

    @MockBean
    private EventStatsAdapter eventStatsAdapter;

    private User alice;
    private User bob;
    private User carol;

    private Category concerts;
    private Category theatre;

    private Event alicePublishedConcert;
    private Event alicePendingConcert;
    private Event alicePublishedEarlierConcert;
    private Event bobPublishedTheatre;
    private Event bobCanceledConcert;
    private Event limitedAvailable;
    private Event limitedFull;
    private Event zeroLimit;

    @BeforeEach
    void setUp() {
        when(eventStatsAdapter.getViews(anyList())).thenReturn(Map.of());
        when(eventStatsAdapter.getViews(anyLong())).thenReturn(0L);

        alice = createUser("Alice", "alice@mail.com");
        bob = createUser("Bob", "bob@mail.com");
        carol = createUser("Carol", "carol@mail.com");

        concerts = createCategory("Concerts");
        theatre = createCategory("Theatre");

        alicePublishedConcert = createPublishedEvent(alice, concerts,
                BASE_DATE.plusDays(5),
                "Первый тестовый концерт",
                false, 100);

        alicePublishedEarlierConcert = createPublishedEvent(alice, concerts,
                BASE_DATE.minusDays(5),
                "Ранний концерт",
                false, 100);

        alicePendingConcert = createEvent(alice, concerts, EventState.PENDING,
                BASE_DATE.plusDays(7),
                "Pending концерт",
                false, 50);

        bobPublishedTheatre = createPublishedEvent(bob, theatre,
                BASE_DATE.plusDays(15),
                "Театральная постановка",
                true, 30);

        bobCanceledConcert = createEvent(bob, concerts, EventState.CANCELED,
                BASE_DATE.plusDays(20),
                "Отмененный концерт",
                false, 40);

        limitedAvailable = createPublishedEvent(alice, concerts,
                BASE_DATE.plusDays(9),
                "Доступный лимитированный концерт",
                false, 5);

        limitedFull = createPublishedEvent(alice, concerts,
                BASE_DATE.plusDays(11),
                "Заполненный лимитированный концерт",
                false, 3);

        zeroLimit = createPublishedEvent(alice, concerts,
                BASE_DATE.plusDays(13),
                "Событие без лимита",
                false, 0);
    }

    @Test
    void testFindAllPublicOnlyPublishedReturn() {
        List<EventShortDto> result = eventService.findAllPublic(
                filter(null, null, null, null, null, false),
                params(0, 20, null),
                hit());

        assertThat(result).extracting(EventShortDto::getId)
                .containsExactlyInAnyOrder(
                        alicePublishedConcert.getId(),
                        alicePublishedEarlierConcert.getId(),
                        bobPublishedTheatre.getId(),
                        limitedAvailable.getId(),
                        limitedFull.getId(),
                        zeroLimit.getId()
                )
                .doesNotContain(
                        alicePendingConcert.getId(),
                        bobCanceledConcert.getId()
                );
    }

    @Test
    void testFindAllPublicByText() {
        List<EventShortDto> result = eventService.findAllPublic(
                filter("Театральная", null, null, null, null, false),
                params(0, 20, null),
                hit());

        assertThat(result).extracting(EventShortDto::getId)
                .containsExactly(bobPublishedTheatre.getId());
    }

    @Test
    void testFindAllPublicByCategory() {
        List<EventShortDto> result = eventService.findAllPublic(
                filter(null, List.of(theatre.getId()), null, null, null, false),
                params(0, 20, null),
                hit());

        assertThat(result).extracting(EventShortDto::getId)
                .containsExactly(bobPublishedTheatre.getId());
    }

    @Test
    void testFindAllPublicByPaid() {
        List<EventShortDto> result = eventService.findAllPublic(
                filter(null, null, true, null, null, false),
                params(0, 20, null),
                hit());

        assertThat(result).extracting(EventShortDto::getId)
                .containsExactly(bobPublishedTheatre.getId());
    }

    @Test
    void testFindAllPublicByDateRange() {
        List<EventShortDto> result = eventService.findAllPublic(
                filter(null, null, null, BASE_DATE, BASE_DATE.plusDays(10), false),
                params(0, 20, null),
                hit());

        assertThat(result).extracting(EventShortDto::getId)
                .containsExactlyInAnyOrder(
                        alicePublishedConcert.getId(),
                        limitedAvailable.getId()
                );
    }

    @Test
    void testFindAllPublicOnlyAvailable() {
        createConfirmedRequest(limitedAvailable, bob);
        createConfirmedRequest(limitedAvailable, alice);

        createConfirmedRequest(limitedFull, alice);
        createConfirmedRequest(limitedFull, bob);
        createConfirmedRequest(limitedFull, carol);

        eventRepository.flush();
        requestRepository.flush();

        List<EventShortDto> result = eventService.findAllPublic(
                filter(null, null, null, null, null, true),
                params(0, 20, null),
                hit());

        assertThat(result).extracting(EventShortDto::getId)
                .contains(limitedAvailable.getId())
                .doesNotContain(limitedFull.getId());
    }

    @Test
    void testFindAllPublicOnlyAvailableExcludesEventWithZeroLimit() {
        List<EventShortDto> result = eventService.findAllPublic(
                filter(null, null, null, null, null, true),
                params(0, 20, null),
                hit());

        assertThat(result).extracting(EventShortDto::getId)
                .doesNotContain(zeroLimit.getId());
    }

    @Test
    void testFindAllPublicSortByEventDateDesc() {
        List<EventShortDto> result = eventService.findAllPublic(
                filter(null, null, null, null, null, false),
                params(0, 20, EventSearchSort.EVENT_DATE),
                hit());

        assertThat(result)
                .extracting(EventShortDto::getEventDate)
                .isSortedAccordingTo(Comparator.reverseOrder());
    }

    @Test
    void testFindAllPublicSortByViewsInMemory() {
        when(eventStatsAdapter.getViews(anyList()))
                .thenReturn(Map.of(
                        alicePublishedConcert.getId(), 5L,
                        alicePublishedEarlierConcert.getId(), 100L,
                        bobPublishedTheatre.getId(), 50L,
                        limitedAvailable.getId(), 1L,
                        limitedFull.getId(), 999L,
                        zeroLimit.getId(), 0L
                ));

        List<EventShortDto> result = eventService.findAllPublic(
                filter(null, null, null, null, null, false),
                params(0, 20, EventSearchSort.VIEWS),
                hit());

        assertThat(result).extracting(EventShortDto::getId)
                .containsExactly(
                        limitedFull.getId(),
                        alicePublishedEarlierConcert.getId(),
                        bobPublishedTheatre.getId(),
                        alicePublishedConcert.getId(),
                        limitedAvailable.getId(),
                        zeroLimit.getId()
                );
    }

    @Test
    void testFindAllPublicSortByViewsPaginationAppliedInMemory() {
        when(eventStatsAdapter.getViews(anyList()))
                .thenReturn(Map.of(
                        alicePublishedConcert.getId(), 5L,
                        alicePublishedEarlierConcert.getId(), 100L,
                        bobPublishedTheatre.getId(), 50L,
                        limitedAvailable.getId(), 1L,
                        limitedFull.getId(), 999L,
                        zeroLimit.getId(), 0L
                ));

        List<EventShortDto> result = eventService.findAllPublic(
                filter(null, null, null, null, null, false),
                params(1, 2, EventSearchSort.VIEWS),
                hit());

        assertThat(result).extracting(EventShortDto::getId)
                .containsExactly(
                        alicePublishedEarlierConcert.getId(),
                        bobPublishedTheatre.getId()
                );
    }

    @Test
    void testFindAllPublicPagination() {
        List<EventShortDto> result = eventService.findAllPublic(
                filter(null, null, null, null, null, false),
                params(0, 2, null),
                hit());

        assertThat(result).hasSize(2);
    }

    @Test
    void testFindAllPublicWithAllFilters() {
        List<EventShortDto> result = eventService.findAllPublic(
                filter("концерт", List.of(concerts.getId()), false,
                        BASE_DATE, BASE_DATE.plusDays(20), false),
                params(0, 20, null),
                hit());

        assertThat(result).extracting(EventShortDto::getId)
                .containsExactlyInAnyOrder(
                        alicePublishedConcert.getId(),
                        limitedAvailable.getId(),
                        limitedFull.getId()
                );
    }

    @Test
    void testFindAllPublicWithEmptyResult() {
        List<EventShortDto> result = eventService.findAllPublic(
                filter("несуществующий текст", null, null, null, null, false),
                params(0, 20, null),
                hit());

        assertThat(result).isEmpty();
    }

    @Test
    void testFindByIdPublicReturnsPublishedEvent() {
        createConfirmedRequest(alicePublishedConcert, bob);
        createConfirmedRequest(alicePublishedConcert, carol);

        requestRepository.save(Request.builder()
                .event(alicePublishedConcert)
                .requester(alice)
                .status(RequestStatus.PENDING)
                .created(LocalDateTime.now())
                .build());
        eventRepository.flush();
        requestRepository.flush();

        when(eventStatsAdapter.getViews(alicePublishedConcert.getId())).thenReturn(40L);

        EventFullDto result = eventService.findByIdPublic(alicePublishedConcert.getId(), hit());

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(alicePublishedConcert.getId());
        assertThat(result.getTitle()).isEqualTo(alicePublishedConcert.getTitle());
        assertThat(result.getConfirmedRequests()).isEqualTo(2L);
        assertThat(result.getViews()).isEqualTo(40L);
        assertThat(result.getState()).isEqualTo(EventState.PUBLISHED);

        verify(eventStatsAdapter).getViews(alicePublishedConcert.getId());
        verify(eventStatsAdapter).createHit(URI, IP);
    }

    @Test
    void testFindByIdPublicWithZeroViewsAndRequests() {
        when(eventStatsAdapter.getViews(alicePublishedConcert.getId())).thenReturn(0L);

        EventFullDto result = eventService.findByIdPublic(alicePublishedConcert.getId(), hit());

        assertThat(result.getConfirmedRequests()).isZero();
        assertThat(result.getViews()).isZero();
    }

    @Test
    void testFindByIdPublicPendingEventThrowsNotFound() {
        assertThatThrownBy(() -> eventService.findByIdPublic(alicePendingConcert.getId(), hit()))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("id = " + alicePendingConcert.getId());

        verify(eventStatsAdapter, never()).createHit(any(), any());
    }

    @Test
    void testFindByIdPublicCanceledEventThrowsNotFound() {
        assertThatThrownBy(() -> eventService.findByIdPublic(bobCanceledConcert.getId(), hit()))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("id = " + bobCanceledConcert.getId());

        verify(eventStatsAdapter, never()).createHit(any(), any());
    }

    @Test
    void testFindByIdPublicNonExistentEventThrowsNotFound() {
        assertThatThrownBy(() -> eventService.findByIdPublic(99L, hit()))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("id = " + 99L);

        verify(eventStatsAdapter, never()).createHit(any(), any());
    }

    @Test
    void testFindByIdPublicCreatesHitOnlyForPublished() {
        eventService.findByIdPublic(alicePublishedConcert.getId(), hit());

        verify(eventStatsAdapter).createHit(URI, IP);
    }

    private PublicEventSearchFilter filter(String text, List<Long> categories, Boolean paid,
                                           LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                           Boolean onlyAvailable) {
        return new PublicEventSearchFilter(text, categories, paid, rangeStart, rangeEnd, onlyAvailable);
    }

    private EventPaginationParams params(int from, int size, EventSearchSort sort) {
        return new EventPaginationParams(from, size, sort);
    }

    private EventStatsHitParams hit() {
        return new EventStatsHitParams(URI, IP);
    }

    private User createUser(String name, String email) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        return userRepository.save(user);
    }

    private Category createCategory(String name) {
        Category category = new Category();
        category.setName(name);
        return categoryRepository.save(category);
    }

    private Event createPublishedEvent(User initiator, Category category, LocalDateTime eventDate, String title,
                                       boolean paid, int participantLimit) {
        return createEvent(initiator, category, EventState.PUBLISHED, eventDate,
                title, paid, participantLimit);
    }

    private Event createEvent(User initiator, Category category, EventState state, LocalDateTime eventDate,
                              String title, boolean paid, int participantLimit) {
        Event event = Event.builder()
                .annotation(title)
                .description(title)
                .category(category)
                .eventDate(eventDate)
                .eventLocation(EventLocation.builder().lat(55.75f).lon(37.62f).build())
                .paid(paid)
                .participantLimit(participantLimit)
                .requestModeration(true)
                .title(title)
                .state(state)
                .publishedOn(state == EventState.PUBLISHED ? LocalDateTime.now() : null)
                .initiator(initiator)
                .build();
        return eventRepository.save(event);
    }

    private void createConfirmedRequest(Event event, User requester) {
        Request request = Request.builder()
                .event(event)
                .requester(requester)
                .status(RequestStatus.CONFIRMED)
                .created(LocalDateTime.now())
                .build();
        requestRepository.save(request);
    }
}