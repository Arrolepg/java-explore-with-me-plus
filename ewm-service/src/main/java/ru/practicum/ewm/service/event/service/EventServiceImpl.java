package ru.practicum.ewm.service.event.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.service.category.model.Category;
import ru.practicum.ewm.service.category.query.CategoryQuery;
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
import ru.practicum.ewm.service.event.utility.AdminEventSearchRequest;
import ru.practicum.ewm.service.event.utility.EventMapper;
import ru.practicum.ewm.service.event.utility.PrivateEventSearchRequest;
import ru.practicum.ewm.service.adapter.EventStatsAdapter;
import ru.practicum.ewm.service.event.utility.ResourceReference;
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

        long confirmedRequests = requestQuery.countRequestsByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED);
        long views = eventStatsAdapter.getViews(event);

        return EventMapper.toEventFullDto(event, confirmedRequests, views);
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
    public List<EventShortDto> findAll(Long userId, PrivateEventSearchRequest privateEventSearchRequest) {
        userQuery.checkUserExists(userId);
        Pageable pageable = toPageable(privateEventSearchRequest);

        List<Event> events = eventRepository.findEventsByInitiatorIdWithFetch(userId, pageable);
        if (events.isEmpty()) {
            return List.of();
        }

        List<Long> eventsIds = events.stream()
                .map(Event::getId)
                .toList();
        Map<Long, Long> confirmedRequestsMap = requestQuery.countRequestsByEventIdsAndStatus(eventsIds,
                RequestStatus.CONFIRMED);
        Map<Long, Long> viewsMap = eventStatsAdapter.getViews(events);

        return EventMapper.toListEventShortDto(events, confirmedRequestsMap, viewsMap);
    }

    @Override
    public EventFullDto findById(ResourceReference resourceReference) {
        userQuery.checkUserExists(resourceReference.userId());

        Event event = findFullEvent(resourceReference.eventId());
        checkInitiator(event.getInitiator().getId(), resourceReference.userId());

        long confirmedRequests = requestQuery.countRequestsByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED);
        long views = eventStatsAdapter.getViews(event);

        return EventMapper.toEventFullDto(event, confirmedRequests, views);
    }

    @Override
    public List<EventFullDto> findAllByAdmin(AdminEventSearchRequest searchRequest) {
        validateAdminSearchRequest(searchRequest);

        List<Event> events = eventRepository.findAllWithFetch(EventSpecifications.forAdminSearch(searchRequest),
                searchRequest.from(), searchRequest.size());
        if (events.isEmpty()) {
            return List.of();
        }

        List<Long> eventsIds = events.stream()
                .map(Event::getId)
                .toList();
        Map<Long, Long> confirmedRequestsMap = requestQuery.countRequestsByEventIdsAndStatus(eventsIds,
                RequestStatus.CONFIRMED);
        Map<Long, Long> viewsMap = eventStatsAdapter.getViews(events);

        return events.stream()
                .map(event -> EventMapper.toEventFullDto(
                        event,
                        confirmedRequestsMap.getOrDefault(event.getId(), 0L),
                        viewsMap.getOrDefault(event.getId(), 0L)
                ))
                .toList();
    }

    @Override
    @Transactional
    public EventFullDto updateByAdmin(Long eventId, UpdateEventAdminRequest eventDto) {
        Event event = findFullEvent(eventId);

        checkAdminStateAction(event, eventDto);

        applyUpdates(event, eventDto);
        applyAdminStateAction(event, eventDto.getStateAction());

        long confirmedRequests = requestQuery.countRequestsByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED);
        long views = eventStatsAdapter.getViews(event);

        return EventMapper.toEventFullDto(event, confirmedRequests, views);
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

    private void validateAdminSearchRequest(AdminEventSearchRequest searchRequest) {
        if (searchRequest.from() < 0) {
            throw new BadRequestException("Параметр from не может быть отрицательным");
        }
        if (searchRequest.size() <= 0) {
            throw new BadRequestException("Параметр size должен быть больше нуля");
        }
        if (searchRequest.rangeStart() != null && searchRequest.rangeEnd() != null
                && searchRequest.rangeStart().isAfter(searchRequest.rangeEnd())) {
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

    private Pageable toPageable(PrivateEventSearchRequest privateEventSearchRequest) {
        return PageRequest.of(
                privateEventSearchRequest.from() / privateEventSearchRequest.size(),
                privateEventSearchRequest.size(),
                SORT_BY_ID_ASC
        );
    }
}
