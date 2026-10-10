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
import ru.practicum.ewm.service.event.dto.request.UpdateEventAdminRequest;
import ru.practicum.ewm.service.event.dto.response.EventFullDto;
import ru.practicum.ewm.service.event.model.AdminEventStateAction;
import ru.practicum.ewm.service.event.model.Event;
import ru.practicum.ewm.service.event.model.EventLocation;
import ru.practicum.ewm.service.event.model.EventState;
import ru.practicum.ewm.service.event.repository.EventRepository;
import ru.practicum.ewm.service.event.utility.AdminEventSearchRequest;
import ru.practicum.ewm.service.user.model.User;
import ru.practicum.ewm.service.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class AdminEventServiceImplIntegrationTest {
    private static final LocalDateTime BASE_DATE = LocalDateTime.now().plusDays(30).withNano(0);

    @Autowired
    private AdminEventService adminEventService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @MockBean
    private EventStatsAdapter eventStatsAdapter;

    private User alice;
    private User bob;
    private Category concerts;
    private Category theatre;
    private Event alicePendingConcert;
    private Event alicePublishedTheatre;
    private Event bobCanceledConcert;

    @BeforeEach
    void setUp() {
        when(eventStatsAdapter.getViews(anyList())).thenReturn(Map.of());
        when(eventStatsAdapter.getViews(any(Event.class))).thenReturn(0L);

        alice = createUser("Alice", "alice@mail.com");
        bob = createUser("Bob", "bob@mail.com");
        concerts = createCategory("Concerts");
        theatre = createCategory("Theatre");

        alicePendingConcert = createEvent(alice, concerts, EventState.PENDING, BASE_DATE);
        alicePublishedTheatre = createEvent(alice, theatre, EventState.PUBLISHED, BASE_DATE.plusDays(10));
        bobCanceledConcert = createEvent(bob, concerts, EventState.CANCELED, BASE_DATE.plusDays(20));
    }

    @Test
    void testFindAllWithoutFilters() {
        List<EventFullDto> result = adminEventService.findAll(search(null, null, null, null, null));

        assertThat(result).extracting(EventFullDto::getId)
                .containsExactly(alicePendingConcert.getId(), alicePublishedTheatre.getId(),
                        bobCanceledConcert.getId());
    }

    @Test
    void testFindAllByUsers() {
        List<EventFullDto> result = adminEventService.findAll(search(List.of(bob.getId()), null, null, null, null));

        assertThat(result).extracting(EventFullDto::getId).containsExactly(bobCanceledConcert.getId());
    }

    @Test
    void testFindAllByStates() {
        List<EventFullDto> result = adminEventService.findAll(
                search(null, List.of(EventState.PENDING, EventState.CANCELED), null, null, null));

        assertThat(result).extracting(EventFullDto::getId)
                .containsExactly(alicePendingConcert.getId(), bobCanceledConcert.getId());
    }

    @Test
    void testFindAllByCategories() {
        List<EventFullDto> result = adminEventService.findAll(
                search(null, null, List.of(theatre.getId()), null, null));

        assertThat(result).extracting(EventFullDto::getId).containsExactly(alicePublishedTheatre.getId());
    }

    @Test
    void testFindAllByDateRange() {
        List<EventFullDto> result = adminEventService.findAll(
                search(null, null, null, BASE_DATE.plusDays(5), BASE_DATE.plusDays(15)));

        assertThat(result).extracting(EventFullDto::getId).containsExactly(alicePublishedTheatre.getId());
    }

    @Test
    void testFindAllWithCombinedFilters() {
        List<EventFullDto> result = adminEventService.findAll(
                search(List.of(alice.getId()), List.of(EventState.PENDING), List.of(concerts.getId()),
                        BASE_DATE.minusDays(1), BASE_DATE.plusDays(1)));

        assertThat(result).extracting(EventFullDto::getId).containsExactly(alicePendingConcert.getId());
    }

    @Test
    void testFindAllWithPagination() {
        List<EventFullDto> result = adminEventService.findAll(
                new AdminEventSearchRequest(null, null, null, null, null, 2, 2));

        assertThat(result).extracting(EventFullDto::getId).containsExactly(bobCanceledConcert.getId());
    }

    @Test
    void testFindAllReturnsCategoryAndInitiator() {
        List<EventFullDto> result = adminEventService.findAll(
                search(List.of(bob.getId()), null, null, null, null));

        assertAll(
                () -> assertThat(result.getFirst().getCategory().getName()).isEqualTo("Concerts"),
                () -> assertThat(result.getFirst().getInitiator().getName()).isEqualTo("Bob")
        );
    }

    @Test
    void testPublishIsSaved() {
        UpdateEventAdminRequest request = new UpdateEventAdminRequest();
        request.setStateAction(AdminEventStateAction.PUBLISH_EVENT);
        request.setTitle("Published title");

        adminEventService.update(alicePendingConcert.getId(), request);
        eventRepository.flush();

        Event saved = eventRepository.findById(alicePendingConcert.getId()).orElseThrow();
        assertAll(
                () -> assertThat(saved.getState()).isEqualTo(EventState.PUBLISHED),
                () -> assertThat(saved.getPublishedOn()).isNotNull(),
                () -> assertThat(saved.getTitle()).isEqualTo("Published title")
        );
    }

    private AdminEventSearchRequest search(List<Long> users, List<EventState> states, List<Long> categories,
                                           LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        return new AdminEventSearchRequest(users, states, categories, rangeStart, rangeEnd, 0, 10);
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

    private Event createEvent(User initiator, Category category, EventState state, LocalDateTime eventDate) {
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
                .state(state)
                .createdOn(LocalDateTime.now())
                .publishedOn(state == EventState.PUBLISHED ? LocalDateTime.now() : null)
                .initiator(initiator)
                .build();
        return eventRepository.save(event);
    }
}
