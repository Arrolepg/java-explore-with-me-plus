package ru.practicum.ewm.service.event.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.service.category.model.Category;
import ru.practicum.ewm.service.category.query.CategoryQuery;
import ru.practicum.ewm.service.comment.dto.response.CommentEventResponseDto;
import ru.practicum.ewm.service.comment.model.CommentState;
import ru.practicum.ewm.service.comment.query.CommentQuery;
import ru.practicum.ewm.service.event.utility.EventPaginationParams;
import ru.practicum.ewm.service.event.dto.request.EventLocationUpdateDto;
import ru.practicum.ewm.service.event.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.service.event.dto.request.NewEventDto;
import ru.practicum.ewm.service.event.dto.request.UpdateEventAdminRequest;
import ru.practicum.ewm.service.event.dto.request.UpdateEventRequest;
import ru.practicum.ewm.service.event.dto.request.UpdateEventUserRequest;
import ru.practicum.ewm.service.event.dto.response.EventFullDto;
import ru.practicum.ewm.service.event.dto.response.EventRequestStatusUpdateResult;
import ru.practicum.ewm.service.event.dto.response.EventShortDto;
import ru.practicum.ewm.service.event.model.*;
import ru.practicum.ewm.service.event.query.EventQuery;
import ru.practicum.ewm.service.event.repository.EventRepository;
import ru.practicum.ewm.service.event.repository.EventSpecifications;
import ru.practicum.ewm.service.event.utility.*;
import ru.practicum.ewm.service.adapter.EventStatsAdapter;
import ru.practicum.ewm.service.exception.BadRequestException;
import ru.practicum.ewm.service.exception.ConflictException;
import ru.practicum.ewm.service.exception.NotFoundException;
import ru.practicum.ewm.service.request.command.RequestCommandService;
import ru.practicum.ewm.service.request.command.RequestStatusUpdateCommand;
import ru.practicum.ewm.service.request.command.RequestStatusUpdateResult;
import ru.practicum.ewm.service.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.service.request.model.RequestStatus;
import ru.practicum.ewm.service.request.query.RequestQuery;
import ru.practicum.ewm.service.user.model.User;
import ru.practicum.ewm.service.user.query.UserQuery;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {
    private static final Sort SORT_BY_ID_ASC = Sort.by(Sort.Direction.ASC, Event_.ID);
    private static final long MIN_HOURS_BEFORE_EVENT_ON_PUBLISH = 1;

    private final EventRepository eventRepository;
    private final UserQuery userQuery;
    private final CategoryQuery categoryQuery;
    private final RequestQuery requestQuery;
    private final EventQuery eventQuery;
    private final CommentQuery commentQuery;

    private final RequestCommandService requestCommandService;

    private final EventStatsAdapter eventStatsAdapter;

    @Override
    @Transactional
    public EventFullDto create(Long userId, NewEventDto eventDto) {
        User initiator = userQuery.findUser(userId);
        Category category = categoryQuery.findCategory(eventDto.getCategoryId());

        Event event = EventMapper.toEvent(category, initiator, eventDto);
        return EventMapper.toEventFullDto(eventRepository.save(event));
    }

    @Override
    @Transactional
    public EventFullDto update(ResourceReference resourceReference, UpdateEventUserRequest eventDto) {
        userQuery.checkUserExists(resourceReference.userId());

        Event event = findFullEvent(resourceReference.eventId());

        checkInitiator(event.getInitiator().getId(), resourceReference.userId());
        checkUpdatableEventState(event.getState());

        applyUpdates(event, eventDto);
        applyUserStateAction(event, eventDto.getStateAction());

        return EventMapper.toEventFullDto(event);
    }

    @Override
    public List<ParticipationRequestDto> findRequests(ResourceReference resourceReference) {
        userQuery.checkUserExists(resourceReference.userId());

        Event event = eventQuery.findEvent(resourceReference.eventId());
        checkInitiator(event.getInitiator().getId(), resourceReference.userId());

        return requestQuery.findRequests(resourceReference.eventId());
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateRequests(ResourceReference resourceReference,
                                                         EventRequestStatusUpdateRequest statusUpdateRequestDto) {
        userQuery.checkUserExists(resourceReference.userId());

        Event event = eventQuery.findEvent(resourceReference.eventId());
        checkInitiator(event.getInitiator().getId(), resourceReference.userId());

        RequestStatusUpdateCommand command = new RequestStatusUpdateCommand(
                resourceReference.eventId(),
                statusUpdateRequestDto.getRequestIds(),
                statusUpdateRequestDto.getStatus(),
                event.getParticipantLimit()
        );
        RequestStatusUpdateResult updateResult = requestCommandService.updateRequestsStatuses(command);

        return EventMapper.toEventRequestStatusUpdateResult(updateResult);
    }

    @Override
    public List<EventShortDto> findAllByInitiator(Long userId, EventPaginationParams eventPaginationParams) {
        userQuery.checkUserExists(userId);
        Pageable pageable = toPageable(eventPaginationParams);

        List<Event> events = eventRepository.findEventsByInitiatorIdWithFetch(userId, pageable);

        return eventQuery.getEventsDto(events);
    }

    @Override
    public EventFullDto findById(ResourceReference resourceReference) {
        userQuery.checkUserExists(resourceReference.userId());

        Event event = findFullEvent(resourceReference.eventId());
        checkInitiator(event.getInitiator().getId(), resourceReference.userId());

        long confirmedRequests = requestQuery.countRequestsByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED);
        long views = eventStatsAdapter.getViews(event.getId());
        List<CommentEventResponseDto> comments = commentQuery.findCommentsWithAuthorByEventIdAndState(
                event.getId(),
                CommentState.PUBLISHED
        );

        return EventMapper.toEventFullDto(event, confirmedRequests, views, comments);
    }

    @Override
    public List<EventFullDto> findAllByAdmin(AdminEventSearchFilter searchFilter, EventPaginationParams eventPaginationParams) {
        validateSearchRequestDateRange(searchFilter.rangeStart(), searchFilter.rangeEnd());

        List<Event> events = eventRepository.findAllWithFetch(EventSpecifications.forAdminSearch(searchFilter),
                toPageable(eventPaginationParams));
        if (events.isEmpty()) {
            return List.of();
        }

        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .toList();
        Map<Long, Long> confirmedRequestsMap = requestQuery.countRequestsByEventIdsAndStatus(eventIds,
                RequestStatus.CONFIRMED);
        Map<Long, Long> viewsMap = eventStatsAdapter.getViews(eventIds);
        Map<Long, List<CommentEventResponseDto>> commentsMap = commentQuery.findCommentsWithAuthorByEventIdsAndState(
                eventIds,
                CommentState.PUBLISHED
        );

        return EventMapper.toListEventFullDto(events, confirmedRequestsMap, viewsMap, commentsMap);
    }

    @Override
    @Transactional
    public EventFullDto updateByAdmin(Long eventId, UpdateEventAdminRequest eventDto) {
        Event event = findFullEvent(eventId);

        checkAdminStateAction(event, eventDto);

        applyUpdates(event, eventDto);
        applyAdminStateAction(event, eventDto.getStateAction());

        long confirmedRequests = requestQuery.countRequestsByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED);
        long views = eventStatsAdapter.getViews(event.getId());
        List<CommentEventResponseDto> comments = commentQuery.findCommentsWithAuthorByEventIdAndState(event.getId(),
                CommentState.PUBLISHED);

        return EventMapper.toEventFullDto(event, confirmedRequests, views, comments);
    }

    @Override
    public List<EventShortDto> findAllPublic(PublicEventSearchFilter searchFilter, EventPaginationParams eventPaginationParams,
                                             EventStatsHitParams hitParams) {
        validateSearchRequestDateRange(searchFilter.rangeStart(), searchFilter.rangeEnd());

        Pageable pageable = toPageable(eventPaginationParams);
        Specification<Event> specification = EventSpecifications.forPublicSearch(searchFilter);
        List<Event> events = eventRepository.findAllWithFetch(specification, pageable);
        if (events.isEmpty()) {
            eventStatsAdapter.createHit(hitParams.uri(), hitParams.ip());
            return List.of();
        }

        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .toList();
        Map<Long, Long> confirmedMap = requestQuery.countRequestsByEventIdsAndStatus(
                eventIds, RequestStatus.CONFIRMED
        );
        Map<Long, Long> viewsMap = eventStatsAdapter.getViews(eventIds);
        Map<Long, Long> commentsMap = commentQuery.countCommentsByEventIdsAndState(eventIds, CommentState.PUBLISHED);

        if (eventPaginationParams.sort() == EventSearchSort.VIEWS) {
            events = events.stream()
                    .sorted(Comparator.comparingLong(
                            (Event e) -> viewsMap.getOrDefault(e.getId(), 0L)
                    ).reversed())
                    .skip(eventPaginationParams.from())
                    .limit(eventPaginationParams.size())
                    .toList();
        }

        eventStatsAdapter.createHit(hitParams.uri(), hitParams.ip());

        return EventMapper.toListEventShortDto(events, confirmedMap, viewsMap, commentsMap);
    }

    @Override
    public EventFullDto findByIdPublic(Long eventId, EventStatsHitParams hitParams) {
        Event event = findFullEvent(eventId);
        checkPublishedEvent(event);

        eventStatsAdapter.createHit(hitParams.uri(), hitParams.ip());

        long confirmedRequests = requestQuery.countRequestsByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED);
        Long views = eventStatsAdapter.getViews(event.getId());
        List<CommentEventResponseDto> comments = commentQuery.findCommentsWithAuthorByEventIdAndState(
                event.getId(),
                CommentState.PUBLISHED
        );

        return EventMapper.toEventFullDto(event, confirmedRequests, views, comments);
    }

    private void checkInitiator(Long initiatorId, Long userId) {
        if (!initiatorId.equals(userId)) {
            throw new ConflictException("Пользователь не является инициатором события");
        }
    }

    private void checkUpdatableEventState(EventState eventState) {
        if (eventState.equals(EventState.PUBLISHED)) {
            throw new ConflictException("Редактировать можно только события в состоянии " +
                    EventState.PENDING + " и " + EventState.CANCELED);
        }
    }

    private Event findFullEvent(Long eventId) {
        return eventRepository.findByIdWithFetch(eventId)
                .orElseThrow(
                        () -> new NotFoundException("Событие с id = " + eventId + " не найдено")
                );
    }

    private EventLocation updateLocation(EventLocation eventLocation, EventLocationUpdateDto locationDto) {
        if (locationDto.getLat() != null) {
            eventLocation.setLat(locationDto.getLat());
        }
        if (locationDto.getLon() != null) {
            eventLocation.setLon(locationDto.getLon());
        }

        return eventLocation;
    }

    private void applyUpdates(Event event, UpdateEventRequest eventDto) {
        if (eventDto.getAnnotation() != null) {
            event.setAnnotation(eventDto.getAnnotation());
        }
        if (eventDto.getCategoryId() != null) {
            Category category = categoryQuery.findCategory(eventDto.getCategoryId());
            event.setCategory(category);
        }
        if (eventDto.getDescription() != null) {
            event.setDescription(eventDto.getDescription());
        }
        if (eventDto.getEventDate() != null) {
            event.setEventDate(eventDto.getEventDate());
        }
        if (eventDto.getLocation() != null) {
            event.setEventLocation(updateLocation(event.getEventLocation(), eventDto.getLocation()));
        }
        if (eventDto.getPaid() != null) {
            event.setPaid(eventDto.getPaid());
        }
        if (eventDto.getParticipantLimit() != null) {
            event.setParticipantLimit(eventDto.getParticipantLimit());
        }
        if (eventDto.getRequestModeration() != null) {
            event.setRequestModeration(eventDto.getRequestModeration());
        }
        if (eventDto.getTitle() != null) {
            event.setTitle(eventDto.getTitle());
        }
    }

    private void applyUserStateAction(Event event, UserEventStateAction stateAction) {
        if (stateAction == null) {
            return;
        }
        switch (stateAction) {
            case SEND_TO_REVIEW -> event.setState(EventState.PENDING);
            case CANCEL_REVIEW -> event.setState(EventState.CANCELED);
        }
    }

    private void validateSearchRequestDateRange(LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            throw new BadRequestException("Дата начала диапазона (rangeStart) не может быть позже даты конца (rangeEnd)");
        }
    }

    private void checkAdminStateAction(Event event, UpdateEventAdminRequest eventDto) {
        AdminEventStateAction stateAction = eventDto.getStateAction();
        if (stateAction == null) {
            return;
        }

        if (stateAction == AdminEventStateAction.PUBLISH_EVENT) {
            if (event.getState() != EventState.PENDING) {
                throw new ConflictException("Нельзя опубликовать событие, так как оно не в состоянии "
                        + EventState.PENDING + ": " + event.getState());
            }

            LocalDateTime eventDate = eventDto.getEventDate() != null ? eventDto.getEventDate() : event.getEventDate();
            if (eventDate.isBefore(LocalDateTime.now().plusHours(MIN_HOURS_BEFORE_EVENT_ON_PUBLISH))) {
                throw new ConflictException("Дата начала события должна быть не ранее чем через "
                        + MIN_HOURS_BEFORE_EVENT_ON_PUBLISH + " ч. от даты публикации");
            }
        } else if (stateAction == AdminEventStateAction.REJECT_EVENT && event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Нельзя отклонить событие, так как оно уже опубликовано");
        }
    }

    private void applyAdminStateAction(Event event, AdminEventStateAction stateAction) {
        if (stateAction == AdminEventStateAction.PUBLISH_EVENT) {
            event.setState(EventState.PUBLISHED);
            event.setPublishedOn(LocalDateTime.now());
        } else if (stateAction == AdminEventStateAction.REJECT_EVENT) {
            event.setState(EventState.CANCELED);
        }
    }

    private Pageable toPageable(EventPaginationParams eventPaginationParams) {
        if (eventPaginationParams.sort() == null) {
            return PageRequest.of(
                    eventPaginationParams.from() / eventPaginationParams.size(),
                    eventPaginationParams.size(),
                    SORT_BY_ID_ASC);
        }

        return switch (eventPaginationParams.sort()) {
            case EVENT_DATE -> PageRequest.of(
                    eventPaginationParams.from() / eventPaginationParams.size(),
                    eventPaginationParams.size(),
                    Sort.by(Sort.Direction.DESC, Event_.EVENT_DATE));
            // запрос на все строки по условиям с дальнейшей сортировкой и отсечением в памяти, т к VIEWS нет в БД
            case VIEWS -> Pageable.unpaged(SORT_BY_ID_ASC);
        };
    }

    private void checkPublishedEvent(Event event) {
        if (event.getPublishedOn() == null || event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Событие с id = " + event.getId() + " не найдено");
        }
    }
}
