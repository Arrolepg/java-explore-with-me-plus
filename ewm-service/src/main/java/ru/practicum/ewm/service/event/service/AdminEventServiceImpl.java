package ru.practicum.ewm.service.event.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.service.adapter.EventStatsAdapter;
import ru.practicum.ewm.service.category.query.CategoryQuery;
import ru.practicum.ewm.service.event.dto.request.EventLocationUpdateDto;
import ru.practicum.ewm.service.event.dto.request.UpdateEventAdminRequest;
import ru.practicum.ewm.service.event.dto.response.EventFullDto;
import ru.practicum.ewm.service.event.model.AdminEventStateAction;
import ru.practicum.ewm.service.event.model.Event;
import ru.practicum.ewm.service.event.model.EventLocation;
import ru.practicum.ewm.service.event.model.EventState;
import ru.practicum.ewm.service.event.model.Event_;
import ru.practicum.ewm.service.event.repository.EventRepository;
import ru.practicum.ewm.service.event.repository.EventSpecifications;
import ru.practicum.ewm.service.event.utility.AdminEventSearchRequest;
import ru.practicum.ewm.service.event.utility.EventMapper;
import ru.practicum.ewm.service.exception.BadRequestException;
import ru.practicum.ewm.service.exception.ConflictException;
import ru.practicum.ewm.service.exception.NotFoundException;
import ru.practicum.ewm.service.request.model.RequestStatus;
import ru.practicum.ewm.service.request.query.RequestQuery;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminEventServiceImpl implements AdminEventService {
    private static final Sort SORT_BY_ID_ASC = Sort.by(Sort.Direction.ASC, Event_.ID);
    private static final long MIN_HOURS_BEFORE_EVENT_ON_PUBLISH = 1;

    private final EventRepository eventRepository;
    private final CategoryQuery categoryQuery;
    private final RequestQuery requestQuery;
    private final EventStatsAdapter eventStatsAdapter;

    @Override
    public List<EventFullDto> findAll(AdminEventSearchRequest searchRequest) {
        validateSearchRequest(searchRequest);

        Pageable pageable = PageRequest.of(searchRequest.from() / searchRequest.size(), searchRequest.size(),
                SORT_BY_ID_ASC);
        List<Event> events = eventRepository.findAll(EventSpecifications.forAdminSearch(searchRequest), pageable)
                .getContent();
        if (events.isEmpty()) {
            return List.of();
        }

        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .toList();
        Map<Long, Long> confirmedRequestsMap = requestQuery.countRequestsByEventIdsAndStatus(eventIds,
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
    public EventFullDto update(Long eventId, UpdateEventAdminRequest eventDto) {
        Event event = eventRepository.findByIdWithFetch(eventId)
                .orElseThrow(
                        () -> new NotFoundException("Событие с id = " + eventId + " не найдено")
                );

        checkStateAction(event, eventDto);

        applyUpdates(event, eventDto);
        applyAdminStateAction(event, eventDto.getStateAction());
        log.info("Администратор обновил событие с id = {}, текущее состояние: {}", eventId, event.getState());

        long confirmedRequests = requestQuery.countRequestsByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED);
        long views = eventStatsAdapter.getViews(event);

        return EventMapper.toEventFullDto(event, confirmedRequests, views);
    }

    private void validateSearchRequest(AdminEventSearchRequest searchRequest) {
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

    private void checkStateAction(Event event, UpdateEventAdminRequest eventDto) {
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

    private void applyUpdates(Event event, UpdateEventAdminRequest eventDto) {
        if (eventDto.getAnnotation() != null) {
            event.setAnnotation(eventDto.getAnnotation());
        }
        if (eventDto.getCategoryId() != null) {
            event.setCategory(categoryQuery.findCategory(eventDto.getCategoryId()));
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

    private EventLocation updateLocation(EventLocation eventLocation, EventLocationUpdateDto locationDto) {
        if (locationDto.getLat() != null) {
            eventLocation.setLat(locationDto.getLat());
        }
        if (locationDto.getLon() != null) {
            eventLocation.setLon(locationDto.getLon());
        }

        return eventLocation;
    }
}
