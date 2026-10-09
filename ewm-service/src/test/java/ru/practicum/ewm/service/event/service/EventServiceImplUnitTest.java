package ru.practicum.ewm.service.event.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import ru.practicum.ewm.service.category.model.Category;
import ru.practicum.ewm.service.category.query.CategoryQuery;
import ru.practicum.ewm.service.event.dto.request.EventLocationUpdateDto;
import ru.practicum.ewm.service.event.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.service.event.dto.request.NewEventDto;
import ru.practicum.ewm.service.event.dto.request.UpdateEventUserRequest;
import ru.practicum.ewm.service.event.dto.response.EventFullDto;
import ru.practicum.ewm.service.event.dto.response.EventRequestStatusUpdateResult;
import ru.practicum.ewm.service.event.dto.response.EventShortDto;
import ru.practicum.ewm.service.event.model.Event;
import ru.practicum.ewm.service.event.model.EventLocation;
import ru.practicum.ewm.service.event.model.EventState;
import ru.practicum.ewm.service.event.model.UserEventStateAction;
import ru.practicum.ewm.service.event.query.EventQuery;
import ru.practicum.ewm.service.event.repository.EventRepository;
import ru.practicum.ewm.service.event.utility.PrivateEventSearchRequest;
import ru.practicum.ewm.service.adapter.EventStatsAdapter;
import ru.practicum.ewm.service.event.utility.ResourceReference;
import ru.practicum.ewm.service.exception.ConflictException;
import ru.practicum.ewm.service.exception.NotFoundException;
import ru.practicum.ewm.service.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.service.request.model.Request;
import ru.practicum.ewm.service.request.model.RequestStatus;
import ru.practicum.ewm.service.request.query.RequestQuery;
import ru.practicum.ewm.service.user.model.User;
import ru.practicum.ewm.service.user.query.UserQuery;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EventServiceImplUnitTest {
    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long EVENT_ID = 1L;
    private static final Long CATEGORY_ID = 1L;
    private static final Long REQUEST_ID = 1L;
    private static final String TITLE = "Event title";
    private static final String ANNOTATION = "Some annotation text here for event";
    private static final String DESCRIPTION = "Some description text here for event";

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserQuery userQuery;

    @Mock
    private CategoryQuery categoryQuery;

    @Mock
    private RequestQuery requestQuery;

    @Mock
    private EventStatsAdapter eventStatsAdapter;

    @Mock
    private EventQuery eventQuery;

    @InjectMocks
    private EventServiceImpl eventService;

    @Test
    void testCreate() {
        User initiator = createUser(USER_ID);
        Category category = createCategory(CATEGORY_ID);
        when(userQuery.findUser(USER_ID)).thenReturn(initiator);
        when(categoryQuery.findCategory(CATEGORY_ID)).thenReturn(category);
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> {
            Event e = inv.getArgument(0);
            e.setId(EVENT_ID);
            return e;
        });

        EventFullDto result = eventService.create(USER_ID, createNewEventDto());

        assertThat(result.getId()).isEqualTo(EVENT_ID);
        assertThat(result.getTitle()).isEqualTo(TITLE);

        verify(userQuery, times(1)).findUser(USER_ID);
        verify(categoryQuery, times(1)).findCategory(CATEGORY_ID);
        verify(eventRepository, times(1)).save(any(Event.class));
        verifyNoInteractions(requestQuery, eventStatsAdapter);
    }

    @Test
    void testCreateUserNotFound() {
        when(userQuery.findUser(99L)).thenThrow(new NotFoundException(""));

        assertThatThrownBy(() -> eventService.create(99L, createNewEventDto()))
                .isInstanceOf(NotFoundException.class);

        verify(eventRepository, never()).save(any());
        verifyNoInteractions(categoryQuery, requestQuery, eventStatsAdapter);
    }

    @Test
    void testUpdate() {
        Event event = createEvent(EVENT_ID, USER_ID, EventState.PENDING);
        Category category = createCategory(CATEGORY_ID);
        when(eventRepository.findByIdWithFetch(EVENT_ID)).thenReturn(Optional.of(event));
        when(categoryQuery.findCategory(CATEGORY_ID)).thenReturn(category);
        when(requestQuery.countRequestsByEventIdAndStatus(EVENT_ID, RequestStatus.CONFIRMED)).thenReturn(5L);
        when(eventStatsAdapter.getViews(event)).thenReturn(10L);

        EventFullDto result = eventService.update(
                new ResourceReference(USER_ID, EVENT_ID), createUpdateEventUserRequest());

        assertThat(result.getId()).isEqualTo(EVENT_ID);
        assertThat(result.getViews()).isEqualTo(10L);
        assertThat(result.getConfirmedRequests()).isEqualTo(5L);

        verify(userQuery, times(1)).checkUserExists(USER_ID);
        verify(eventRepository, times(1)).findByIdWithFetch(EVENT_ID);
        verify(requestQuery, times(1)).countRequestsByEventIdAndStatus(EVENT_ID, RequestStatus.CONFIRMED);
        verify(eventStatsAdapter, times(1)).getViews(event);
    }

    @Test
    void testUpdateUserNotFound() {
        doThrow(new NotFoundException("")).when(userQuery).checkUserExists(99L);

        assertThatThrownBy(() -> eventService.update(
                new ResourceReference(99L, EVENT_ID), createUpdateEventUserRequest()))
                .isInstanceOf(NotFoundException.class);

        verify(eventRepository, never()).findByIdWithFetch(any());
    }

    @Test
    void testUpdateEventNotFound() {
        when(eventRepository.findByIdWithFetch(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.update(
                new ResourceReference(USER_ID, 99L), createUpdateEventUserRequest()))
                .isInstanceOf(NotFoundException.class);

        verify(userQuery, times(1)).checkUserExists(USER_ID);
        verify(requestQuery, never()).countRequestsByEventIdAndStatus(any(), any());
        verify(eventStatsAdapter, never()).getViews(any(Event.class));
    }

    @Test
    void testUpdateNotInitiator() {
        Event event = createEvent(EVENT_ID, OTHER_USER_ID, EventState.PENDING);
        when(eventRepository.findByIdWithFetch(EVENT_ID)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> eventService.update(
                new ResourceReference(USER_ID, EVENT_ID), createUpdateEventUserRequest()))
                .isInstanceOf(ConflictException.class);

        verify(requestQuery, never()).countRequestsByEventIdAndStatus(any(), any());
    }

    @Test
    void testUpdatePublishedState() {
        Event event = createEvent(EVENT_ID, USER_ID, EventState.PUBLISHED);
        when(eventRepository.findByIdWithFetch(EVENT_ID)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> eventService.update(
                new ResourceReference(USER_ID, EVENT_ID), createUpdateEventUserRequest()))
                .isInstanceOf(ConflictException.class);

        verify(requestQuery, never()).countRequestsByEventIdAndStatus(any(), any());
    }

    @Test
    void testUpdateWithStateActionCancelReview() {
        Event event = createEvent(EVENT_ID, USER_ID, EventState.PENDING);
        Category category = createCategory(CATEGORY_ID);
        when(eventRepository.findByIdWithFetch(EVENT_ID)).thenReturn(Optional.of(event));
        when(categoryQuery.findCategory(CATEGORY_ID)).thenReturn(category);
        when(requestQuery.countRequestsByEventIdAndStatus(EVENT_ID, RequestStatus.CONFIRMED)).thenReturn(0L);
        when(eventStatsAdapter.getViews(event)).thenReturn(0L);

        UpdateEventUserRequest dto = createUpdateEventUserRequest();
        dto.setStateAction(UserEventStateAction.CANCEL_REVIEW);

        eventService.update(new ResourceReference(USER_ID, EVENT_ID), dto);

        assertThat(event.getState()).isEqualTo(EventState.CANCELED);
    }

    @Test
    void testFindRequests() {
        Event event = createEvent(EVENT_ID, USER_ID, EventState.PENDING);
        when(eventQuery.findEvent(EVENT_ID)).thenReturn(event);
        when(requestQuery.findRequests(EVENT_ID)).thenReturn(List.of(createParticipationRequestDto()));

        List<ParticipationRequestDto> result = eventService.findRequests(
                new ResourceReference(USER_ID, EVENT_ID));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(REQUEST_ID);

        verify(userQuery, times(1)).checkUserExists(USER_ID);
        verify(eventQuery, times(1)).findEvent(EVENT_ID);
        verify(requestQuery, times(1)).findRequests(EVENT_ID);
    }

    @Test
    void testFindRequestsUserNotFound() {
        doThrow(new NotFoundException("")).when(userQuery).checkUserExists(99L);

        assertThatThrownBy(() -> eventService.findRequests(new ResourceReference(99L, EVENT_ID)))
                .isInstanceOf(NotFoundException.class);

        verify(eventRepository, never()).findById(any());
    }

    @Test
    void testFindRequestsEventNotFound() {
        when(eventQuery.findEvent(99L))
                .thenThrow(new NotFoundException("Событие с id = 99 не найдено"));

        assertThatThrownBy(() -> eventService.findRequests(new ResourceReference(USER_ID, 99L)))
                .isInstanceOf(NotFoundException.class);

        verify(requestQuery, never()).findRequests(any());
    }

    @Test
    void testFindRequestsNotInitiator() {
        Event event = createEvent(EVENT_ID, OTHER_USER_ID, EventState.PENDING);
        when(eventQuery.findEvent(EVENT_ID)).thenReturn(event);

        assertThatThrownBy(() -> eventService.findRequests(new ResourceReference(USER_ID, EVENT_ID)))
                .isInstanceOf(ConflictException.class);

        verify(requestQuery, never()).findRequests(any());
    }

    @Test
    void testUpdateRequestsConfirmed() {
        Event event = createEvent(EVENT_ID, USER_ID, EventState.PENDING);
        event.setParticipantLimit(10);
        Request request = createRequest(REQUEST_ID, RequestStatus.PENDING);
        when(eventQuery.findEvent(EVENT_ID)).thenReturn(event);
        when(requestQuery.findRequestsByIdsAndEventId(List.of(REQUEST_ID), EVENT_ID))
                .thenReturn(List.of(request));
        when(requestQuery.countRequestsByEventIdAndStatus(EVENT_ID, RequestStatus.CONFIRMED))
                .thenReturn(0L);

        EventRequestStatusUpdateRequest dto = createStatusUpdateRequest(RequestStatus.CONFIRMED);
        EventRequestStatusUpdateResult result = eventService.updateRequests(
                new ResourceReference(USER_ID, EVENT_ID), dto);

        assertThat(result.getConfirmedRequests()).hasSize(1);
        assertThat(result.getRejectedRequests()).isEmpty();
        assertThat(request.getStatus()).isEqualTo(RequestStatus.CONFIRMED);
    }

    @Test
    void testUpdateRequestsRejected() {
        Event event = createEvent(EVENT_ID, USER_ID, EventState.PENDING);
        event.setParticipantLimit(10);
        Request request = createRequest(REQUEST_ID, RequestStatus.PENDING);
        when(eventQuery.findEvent(EVENT_ID)).thenReturn(event);
        when(requestQuery.findRequestsByIdsAndEventId(List.of(REQUEST_ID), EVENT_ID))
                .thenReturn(List.of(request));
        when(requestQuery.countRequestsByEventIdAndStatus(EVENT_ID, RequestStatus.CONFIRMED))
                .thenReturn(0L);

        EventRequestStatusUpdateRequest dto = createStatusUpdateRequest(RequestStatus.REJECTED);
        EventRequestStatusUpdateResult result = eventService.updateRequests(
                new ResourceReference(USER_ID, EVENT_ID), dto);

        assertThat(result.getRejectedRequests()).hasSize(1);
        assertThat(result.getConfirmedRequests()).isEmpty();
        assertThat(request.getStatus()).isEqualTo(RequestStatus.REJECTED);
    }

    @Test
    void testUpdateRequestsNotPending() {
        Event event = createEvent(EVENT_ID, USER_ID, EventState.PENDING);
        event.setParticipantLimit(10);
        Request request = createRequest(REQUEST_ID, RequestStatus.CONFIRMED);
        when(eventQuery.findEvent(EVENT_ID)).thenReturn(event);
        when(requestQuery.findRequestsByIdsAndEventId(List.of(REQUEST_ID), EVENT_ID))
                .thenReturn(List.of(request));

        EventRequestStatusUpdateRequest dto = createStatusUpdateRequest(RequestStatus.CONFIRMED);

        assertThatThrownBy(() -> eventService.updateRequests(
                new ResourceReference(USER_ID, EVENT_ID), dto))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void testUpdateRequestsLimitReached() {
        Event event = createEvent(EVENT_ID, USER_ID, EventState.PENDING);
        event.setParticipantLimit(5);
        Request request = createRequest(REQUEST_ID, RequestStatus.PENDING);
        when(eventQuery.findEvent(EVENT_ID)).thenReturn(event);
        when(requestQuery.findRequestsByIdsAndEventId(List.of(REQUEST_ID), EVENT_ID))
                .thenReturn(List.of(request));
        when(requestQuery.countRequestsByEventIdAndStatus(EVENT_ID, RequestStatus.CONFIRMED))
                .thenReturn(5L);

        EventRequestStatusUpdateRequest dto = createStatusUpdateRequest(RequestStatus.CONFIRMED);

        assertThatThrownBy(() -> eventService.updateRequests(
                new ResourceReference(USER_ID, EVENT_ID), dto))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void testUpdateRequestsUserNotFound() {
        doThrow(new NotFoundException("")).when(userQuery).checkUserExists(99L);

        assertThatThrownBy(() -> eventService.updateRequests(
                new ResourceReference(99L, EVENT_ID), createStatusUpdateRequest(RequestStatus.CONFIRMED)))
                .isInstanceOf(NotFoundException.class);

        verify(eventRepository, never()).findById(any());
    }

    @Test
    void testFindAll() {
        Event event = createEvent(EVENT_ID, USER_ID, EventState.PENDING);
        when(eventRepository.findEventsByInitiatorIdWithFetch(eq(USER_ID), any(Pageable.class)))
                .thenReturn(List.of(event));
        when(requestQuery.countRequestsByEventIdsAndStatus(anyList(), eq(RequestStatus.CONFIRMED)))
                .thenReturn(Map.of(EVENT_ID, 3L));
        when(eventStatsAdapter.getViews(anyList())).thenReturn(Map.of(EVENT_ID, 7L));

        List<EventShortDto> result = eventService.findAll(USER_ID,
                new PrivateEventSearchRequest(0, 10));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(EVENT_ID);
        assertThat(result.getFirst().getViews()).isEqualTo(7L);
        assertThat(result.getFirst().getConfirmedRequests()).isEqualTo(3L);

        verify(userQuery, times(1)).checkUserExists(USER_ID);
        verify(eventRepository, times(1)).findEventsByInitiatorIdWithFetch(eq(USER_ID), any(Pageable.class));
        verify(requestQuery, times(1)).countRequestsByEventIdsAndStatus(anyList(), eq(RequestStatus.CONFIRMED));
        verify(eventStatsAdapter, times(1)).getViews(anyList());
    }

    @Test
    void testFindAllEmpty() {
        when(eventRepository.findEventsByInitiatorIdWithFetch(eq(USER_ID), any(Pageable.class)))
                .thenReturn(List.of());

        List<EventShortDto> result = eventService.findAll(USER_ID,
                new PrivateEventSearchRequest(0, 10));

        assertThat(result).isEmpty();

        verify(userQuery, times(1)).checkUserExists(USER_ID);
        verify(requestQuery, never()).countRequestsByEventIdsAndStatus(anyList(), any());
        verify(eventStatsAdapter, never()).getViews(anyList());
    }

    @Test
    void testFindAllUserNotFound() {
        doThrow(new NotFoundException("")).when(userQuery).checkUserExists(99L);

        assertThatThrownBy(() -> eventService.findAll(99L,
                new PrivateEventSearchRequest(0, 10)))
                .isInstanceOf(NotFoundException.class);

        verify(eventRepository, never()).findEventsByInitiatorIdWithFetch(any(), any());
    }

    @Test
    void testFindById() {
        Event event = createEvent(EVENT_ID, USER_ID, EventState.PENDING);
        when(eventRepository.findByIdWithFetch(EVENT_ID)).thenReturn(Optional.of(event));
        when(requestQuery.countRequestsByEventIdAndStatus(EVENT_ID, RequestStatus.CONFIRMED)).thenReturn(3L);
        when(eventStatsAdapter.getViews(event)).thenReturn(7L);

        EventFullDto result = eventService.findById(new ResourceReference(USER_ID, EVENT_ID));

        assertThat(result.getId()).isEqualTo(EVENT_ID);
        assertThat(result.getViews()).isEqualTo(7L);
        assertThat(result.getConfirmedRequests()).isEqualTo(3L);

        verify(userQuery, times(1)).checkUserExists(USER_ID);
        verify(eventRepository, times(1)).findByIdWithFetch(EVENT_ID);
    }

    @Test
    void testFindByIdUserNotFound() {
        doThrow(new NotFoundException("")).when(userQuery).checkUserExists(99L);

        assertThatThrownBy(() -> eventService.findById(new ResourceReference(99L, EVENT_ID)))
                .isInstanceOf(NotFoundException.class);

        verify(eventRepository, never()).findByIdWithFetch(any());
    }

    @Test
    void testFindByIdEventNotFound() {
        when(eventRepository.findByIdWithFetch(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.findById(new ResourceReference(USER_ID, 99L)))
                .isInstanceOf(NotFoundException.class);

        verify(requestQuery, never()).countRequestsByEventIdAndStatus(any(), any());
    }

    @Test
    void testFindByIdNotInitiator() {
        Event event = createEvent(EVENT_ID, OTHER_USER_ID, EventState.PENDING);
        when(eventRepository.findByIdWithFetch(EVENT_ID)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> eventService.findById(new ResourceReference(USER_ID, EVENT_ID)))
                .isInstanceOf(ConflictException.class);

        verify(requestQuery, never()).countRequestsByEventIdAndStatus(any(), any());
    }

    @Test
    void testUpdateLocationBothFields() {
        Event event = createEvent(EVENT_ID, USER_ID, EventState.PENDING);
        Category category = createCategory(CATEGORY_ID);
        when(eventRepository.findByIdWithFetch(EVENT_ID)).thenReturn(Optional.of(event));
        when(categoryQuery.findCategory(CATEGORY_ID)).thenReturn(category);
        when(requestQuery.countRequestsByEventIdAndStatus(EVENT_ID, RequestStatus.CONFIRMED)).thenReturn(0L);
        when(eventStatsAdapter.getViews(event)).thenReturn(0L);

        UpdateEventUserRequest dto = createUpdateEventUserRequest();
        EventLocationUpdateDto locationDto = new EventLocationUpdateDto();
        locationDto.setLat(10.5f);
        locationDto.setLon(20.5f);
        dto.setLocation(locationDto);

        eventService.update(new ResourceReference(USER_ID, EVENT_ID), dto);

        assertThat(event.getEventLocation().getLat()).isEqualTo(10.5f);
        assertThat(event.getEventLocation().getLon()).isEqualTo(20.5f);
    }

    @Test
    void testUpdateLocationOnlyLat() {
        Event event = createEvent(EVENT_ID, USER_ID, EventState.PENDING);
        Float oldLon = event.getEventLocation().getLon();
        Category category = createCategory(CATEGORY_ID);
        when(eventRepository.findByIdWithFetch(EVENT_ID)).thenReturn(Optional.of(event));
        when(categoryQuery.findCategory(CATEGORY_ID)).thenReturn(category);
        when(requestQuery.countRequestsByEventIdAndStatus(EVENT_ID, RequestStatus.CONFIRMED)).thenReturn(0L);
        when(eventStatsAdapter.getViews(event)).thenReturn(0L);

        UpdateEventUserRequest dto = createUpdateEventUserRequest();
        EventLocationUpdateDto locationDto = new EventLocationUpdateDto();
        locationDto.setLat(10.5f);
        locationDto.setLon(null);
        dto.setLocation(locationDto);

        eventService.update(new ResourceReference(USER_ID, EVENT_ID), dto);

        assertThat(event.getEventLocation().getLat()).isEqualTo(10.5f);
        assertThat(event.getEventLocation().getLon()).isEqualTo(oldLon);
    }

    @Test
    void testUpdateLocationOnlyLon() {
        Event event = createEvent(EVENT_ID, USER_ID, EventState.PENDING);
        Float oldLat = event.getEventLocation().getLat();
        Category category = createCategory(CATEGORY_ID);
        when(eventRepository.findByIdWithFetch(EVENT_ID)).thenReturn(Optional.of(event));
        when(categoryQuery.findCategory(CATEGORY_ID)).thenReturn(category);
        when(requestQuery.countRequestsByEventIdAndStatus(EVENT_ID, RequestStatus.CONFIRMED)).thenReturn(0L);
        when(eventStatsAdapter.getViews(event)).thenReturn(0L);

        UpdateEventUserRequest dto = createUpdateEventUserRequest();
        EventLocationUpdateDto locationDto = new EventLocationUpdateDto();
        locationDto.setLat(null);
        locationDto.setLon(20.5f);
        dto.setLocation(locationDto);

        eventService.update(new ResourceReference(USER_ID, EVENT_ID), dto);

        assertThat(event.getEventLocation().getLat()).isEqualTo(oldLat);
        assertThat(event.getEventLocation().getLon()).isEqualTo(20.5f);
    }

    @Test
    void testUpdateLocationBothNull() {
        Event event = createEvent(EVENT_ID, USER_ID, EventState.PENDING);
        Float oldLat = event.getEventLocation().getLat();
        Float oldLon = event.getEventLocation().getLon();
        Category category = createCategory(CATEGORY_ID);
        when(eventRepository.findByIdWithFetch(EVENT_ID)).thenReturn(Optional.of(event));
        when(categoryQuery.findCategory(CATEGORY_ID)).thenReturn(category);
        when(requestQuery.countRequestsByEventIdAndStatus(EVENT_ID, RequestStatus.CONFIRMED)).thenReturn(0L);
        when(eventStatsAdapter.getViews(event)).thenReturn(0L);

        UpdateEventUserRequest dto = createUpdateEventUserRequest();
        EventLocationUpdateDto locationDto = new EventLocationUpdateDto();
        locationDto.setLat(null);
        locationDto.setLon(null);
        dto.setLocation(locationDto);

        eventService.update(new ResourceReference(USER_ID, EVENT_ID), dto);

        assertThat(event.getEventLocation().getLat()).isEqualTo(oldLat);
        assertThat(event.getEventLocation().getLon()).isEqualTo(oldLon);
    }

    private User createUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setName("User");
        user.setEmail("user@mail.com");
        return user;
    }

    private Category createCategory(Long id) {
        Category category = new Category();
        category.setId(id);
        category.setName("Category");
        return category;
    }

    private Event createEvent(Long id, Long initiatorId, EventState state) {
        return Event.builder()
                .id(id)
                .annotation(ANNOTATION)
                .category(createCategory(CATEGORY_ID))
                .description(DESCRIPTION)
                .eventDate(LocalDateTime.now().plusDays(10))
                .eventLocation(EventLocation.builder().lat(55.75f).lon(37.62f).build())
                .paid(false)
                .participantLimit(10)
                .requestModeration(true)
                .title(TITLE)
                .state(state)
                .createdOn(LocalDateTime.now())
                .initiator(createUser(initiatorId))
                .build();
    }

    private Request createRequest(Long id, RequestStatus status) {
        Request request = new Request();
        request.setId(id);
        request.setEvent(createEvent(EVENT_ID, USER_ID, EventState.PENDING));
        request.setRequester(createUser(OTHER_USER_ID));
        request.setStatus(status);
        request.setCreated(LocalDateTime.now());
        return request;
    }

    private ParticipationRequestDto createParticipationRequestDto() {
        return ParticipationRequestDto.builder()
                .id(REQUEST_ID)
                .created(LocalDateTime.now())
                .eventId(EVENT_ID)
                .requesterId(OTHER_USER_ID)
                .status(RequestStatus.PENDING)
                .build();
    }

    private NewEventDto createNewEventDto() {
        NewEventDto dto = new NewEventDto();
        dto.setAnnotation(ANNOTATION);
        dto.setCategoryId(CATEGORY_ID);
        dto.setDescription(DESCRIPTION);
        dto.setEventDate(LocalDateTime.now().plusDays(10));
        dto.setLocation(new ru.practicum.ewm.service.event.dto.request.EventLocationCreateDto());
        dto.getLocation().setLat(55.75f);
        dto.getLocation().setLon(37.62f);
        dto.setPaid(false);
        dto.setParticipantLimit(10);
        dto.setRequestModeration(true);
        dto.setTitle(TITLE);
        return dto;
    }

    private UpdateEventUserRequest createUpdateEventUserRequest() {
        UpdateEventUserRequest dto = new UpdateEventUserRequest();
        dto.setAnnotation(ANNOTATION);
        dto.setCategoryId(CATEGORY_ID);
        dto.setDescription(DESCRIPTION);
        dto.setEventDate(LocalDateTime.now().plusDays(10));
        dto.setPaid(false);
        dto.setParticipantLimit(10);
        dto.setRequestModeration(true);
        dto.setTitle(TITLE);
        return dto;
    }

    private EventRequestStatusUpdateRequest createStatusUpdateRequest(RequestStatus status) {
        EventRequestStatusUpdateRequest dto = new EventRequestStatusUpdateRequest();
        dto.setRequestIds(List.of(REQUEST_ID));
        dto.setStatus(status);
        return dto;
    }
}