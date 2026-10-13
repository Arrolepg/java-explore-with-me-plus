package ru.practicum.ewm.service.event.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import ru.practicum.ewm.service.adapter.EventStatsAdapter;
import ru.practicum.ewm.service.category.model.Category;
import ru.practicum.ewm.service.category.query.CategoryQuery;
import ru.practicum.ewm.service.event.dto.request.EventLocationUpdateDto;
import ru.practicum.ewm.service.event.dto.request.UpdateEventAdminRequest;
import ru.practicum.ewm.service.event.dto.response.EventFullDto;
import ru.practicum.ewm.service.event.model.AdminEventStateAction;
import ru.practicum.ewm.service.event.model.Event;
import ru.practicum.ewm.service.event.model.EventLocation;
import ru.practicum.ewm.service.event.model.EventState;
import ru.practicum.ewm.service.event.repository.EventRepository;
import ru.practicum.ewm.service.event.utility.AdminEventSearchFilter;
import ru.practicum.ewm.service.common.PaginationParams;
import ru.practicum.ewm.service.exception.BadRequestException;
import ru.practicum.ewm.service.exception.ConflictException;
import ru.practicum.ewm.service.exception.NotFoundException;
import ru.practicum.ewm.service.request.model.RequestStatus;
import ru.practicum.ewm.service.request.query.RequestQuery;
import ru.practicum.ewm.service.user.model.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EventServiceImplAdminUnitTest {
    private static final Long EVENT_ID = 1L;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private CategoryQuery categoryQuery;

    @Mock
    private RequestQuery requestQuery;

    @Mock
    private EventStatsAdapter eventStatsAdapter;

    @InjectMocks
    private EventServiceImpl eventService;

    @Test
    void testFindAllReturnsEventsWithRequestsAndViews() {
        Event event = createEvent(EventState.PUBLISHED, LocalDateTime.now().plusDays(5));
        when(eventRepository.findAllWithFetch(any(), eq(PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "id")))))
                .thenReturn(List.of(event));
        when(requestQuery.countRequestsByEventIdsAndStatus(List.of(EVENT_ID), RequestStatus.CONFIRMED))
                .thenReturn(Map.of(EVENT_ID, 3L));
        when(eventStatsAdapter.getViews(List.of(event.getId()))).thenReturn(Map.of(EVENT_ID, 7L));

        List<EventFullDto> result = eventService.findAllByAdmin(filter(), params());

        assertThat(result).hasSize(1);
        assertAll(
                () -> assertThat(result.getFirst().getId()).isEqualTo(EVENT_ID),
                () -> assertThat(result.getFirst().getConfirmedRequests()).isEqualTo(3L),
                () -> assertThat(result.getFirst().getViews()).isEqualTo(7L)
        );
    }

    @Test
    void testFindAllEmptyDoesNotRequestStatsAndRequests() {
        when(eventRepository.findAllWithFetch(any(), any(Pageable.class))).thenReturn(List.of());

        List<EventFullDto> result = eventService.findAllByAdmin(filter(), params());

        assertThat(result).isEmpty();
        verifyNoInteractions(requestQuery, eventStatsAdapter);
    }

    @Test
    void testFindAllWithStartAfterEndThrowsBadRequest() {
        AdminEventSearchFilter request = new AdminEventSearchFilter(null, null, null,
                LocalDateTime.of(2031, 1, 1, 0, 0), LocalDateTime.of(2030, 1, 1, 0, 0));
        PaginationParams params = params();

        assertThatThrownBy(() -> eventService.findAllByAdmin(request, params))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(eventRepository);
    }

    @Test
    void testUpdateNotFound() {
        when(eventRepository.findByIdWithFetch(EVENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.updateByAdmin(EVENT_ID, new UpdateEventAdminRequest()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void testPublishPendingEvent() {
        Event event = createEvent(EventState.PENDING, LocalDateTime.now().plusDays(5));
        when(eventRepository.findByIdWithFetch(EVENT_ID)).thenReturn(Optional.of(event));
        stubCounters(event);

        EventFullDto result = eventService.updateByAdmin(EVENT_ID, stateAction(AdminEventStateAction.PUBLISH_EVENT));

        assertAll(
                () -> assertThat(result.getState()).isEqualTo(EventState.PUBLISHED),
                () -> assertThat(result.getPublishedOn()).isNotNull(),
                () -> assertThat(event.getState()).isEqualTo(EventState.PUBLISHED)
        );
    }

    @Test
    void testPublishNotPendingEventThrowsConflict() {
        Event event = createEvent(EventState.CANCELED, LocalDateTime.now().plusDays(5));
        when(eventRepository.findByIdWithFetch(EVENT_ID)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> eventService.updateByAdmin(EVENT_ID, stateAction(AdminEventStateAction.PUBLISH_EVENT)))
                .isInstanceOf(ConflictException.class);
        assertThat(event.getState()).isEqualTo(EventState.CANCELED);
    }

    @Test
    void testPublishEventStartingInLessThanHourThrowsConflict() {
        Event event = createEvent(EventState.PENDING, LocalDateTime.now().plusMinutes(30));
        when(eventRepository.findByIdWithFetch(EVENT_ID)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> eventService.updateByAdmin(EVENT_ID, stateAction(AdminEventStateAction.PUBLISH_EVENT)))
                .isInstanceOf(ConflictException.class);
        assertThat(event.getState()).isEqualTo(EventState.PENDING);
    }

    @Test
    void testPublishWithNewEventDateLessThanHourThrowsConflict() {
        Event event = createEvent(EventState.PENDING, LocalDateTime.now().plusDays(5));
        when(eventRepository.findByIdWithFetch(EVENT_ID)).thenReturn(Optional.of(event));
        UpdateEventAdminRequest request = stateAction(AdminEventStateAction.PUBLISH_EVENT);
        request.setEventDate(LocalDateTime.now().plusMinutes(30));

        assertThatThrownBy(() -> eventService.updateByAdmin(EVENT_ID, request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void testRejectPendingEvent() {
        Event event = createEvent(EventState.PENDING, LocalDateTime.now().plusDays(5));
        when(eventRepository.findByIdWithFetch(EVENT_ID)).thenReturn(Optional.of(event));
        stubCounters(event);

        EventFullDto result = eventService.updateByAdmin(EVENT_ID, stateAction(AdminEventStateAction.REJECT_EVENT));

        assertAll(
                () -> assertThat(result.getState()).isEqualTo(EventState.CANCELED),
                () -> assertThat(result.getPublishedOn()).isNull()
        );
    }

    @Test
    void testRejectPublishedEventThrowsConflict() {
        Event event = createEvent(EventState.PUBLISHED, LocalDateTime.now().plusDays(5));
        when(eventRepository.findByIdWithFetch(EVENT_ID)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> eventService.updateByAdmin(EVENT_ID, stateAction(AdminEventStateAction.REJECT_EVENT)))
                .isInstanceOf(ConflictException.class);
        assertThat(event.getState()).isEqualTo(EventState.PUBLISHED);
    }

    @Test
    void testUpdateFieldsWithoutStateAction() {
        Event event = createEvent(EventState.PENDING, LocalDateTime.now().plusDays(5));
        Category newCategory = createCategory(2L, "New category");
        when(eventRepository.findByIdWithFetch(EVENT_ID)).thenReturn(Optional.of(event));
        when(categoryQuery.findCategory(2L)).thenReturn(newCategory);
        stubCounters(event);

        UpdateEventAdminRequest request = new UpdateEventAdminRequest();
        request.setTitle("New title");
        request.setCategoryId(2L);
        request.setPaid(true);
        EventLocationUpdateDto location = new EventLocationUpdateDto();
        location.setLat(10.5f);
        request.setLocation(location);

        EventFullDto result = eventService.updateByAdmin(EVENT_ID, request);

        assertAll(
                () -> assertThat(result.getTitle()).isEqualTo("New title"),
                () -> assertThat(result.getCategory().getId()).isEqualTo(2L),
                () -> assertThat(result.getPaid()).isTrue(),
                () -> assertThat(result.getLocation().getLat()).isEqualTo(10.5f),
                () -> assertThat(result.getLocation().getLon()).isEqualTo(37.62f),
                () -> assertThat(result.getState()).isEqualTo(EventState.PENDING)
        );
    }

    private void stubCounters(Event event) {
        when(requestQuery.countRequestsByEventIdAndStatus(EVENT_ID, RequestStatus.CONFIRMED)).thenReturn(0L);
        when(eventStatsAdapter.getViews(event.getId())).thenReturn(0L);
    }

    private AdminEventSearchFilter filter() {
        return new AdminEventSearchFilter(null, null, null, null, null);
    }

    private PaginationParams params() {
        return new PaginationParams(0, 10, null);
    }

    private UpdateEventAdminRequest stateAction(AdminEventStateAction stateAction) {
        UpdateEventAdminRequest request = new UpdateEventAdminRequest();
        request.setStateAction(stateAction);
        return request;
    }

    private Event createEvent(EventState state, LocalDateTime eventDate) {
        User initiator = new User();
        initiator.setId(1L);
        initiator.setEmail("user@mail.com");
        initiator.setName("User");
        Category category = createCategory(1L, "Category");
        return Event.builder()
                .id(EVENT_ID)
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
                .initiator(initiator)
                .build();
    }

    private Category createCategory(Long id, String name) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        return category;
    }
}
