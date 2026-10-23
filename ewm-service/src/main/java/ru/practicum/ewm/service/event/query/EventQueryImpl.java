package ru.practicum.ewm.service.event.query;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.service.adapter.EventStatsAdapter;
import ru.practicum.ewm.service.comment.model.CommentState;
import ru.practicum.ewm.service.comment.query.CommentQuery;
import ru.practicum.ewm.service.event.dto.response.EventShortDto;
import ru.practicum.ewm.service.event.model.Event;
import ru.practicum.ewm.service.event.repository.EventRepository;
import ru.practicum.ewm.service.event.utility.EventMapper;
import ru.practicum.ewm.service.exception.NotFoundException;
import ru.practicum.ewm.service.request.model.RequestStatus;
import ru.practicum.ewm.service.request.query.RequestQuery;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class EventQueryImpl implements EventQuery {
    private final EventRepository eventRepository;
    private final RequestQuery requestQuery;
    private final CommentQuery commentQuery;
    private final EventStatsAdapter eventStatsAdapter;

    @Override
    public Event findEvent(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(
                        () -> new NotFoundException("Событие с id = " + eventId + " не найдено")
                );
    }

    @Override
    public boolean existsByCategoryId(Long categoryId) {
        return eventRepository.existsEventByCategoryId(categoryId);
    }

    @Override
    public List<Event> findAllByIdWithFetch(List<Long> eventsIds) {
        return eventRepository.findAllByIdWithFetch(eventsIds);
    }

    @Override
    public List<EventShortDto> getEventsDto(List<Event> events) {
        if (events.isEmpty()) {
            return List.of();
        }
        List<Long> eventsIds = getEventsIds(events);
        return getEventShortDtos(events, eventsIds);
    }

    @Override
    public List<EventShortDto> getEventsDtoByIds(List<Long> eventsIds) {
        List<Event> events = eventRepository.findAllByIdWithFetch(eventsIds);
        if (events.isEmpty()) return List.of();

        return getEventShortDtos(events, eventsIds);
    }

    @Override
    public EventShortDto getEventDtoById(Long eventId) {
        Event event = eventRepository.findByIdWithFetch(eventId)
                .orElseThrow(
                        () -> new NotFoundException("Событие с id = " + eventId + " не найдено")
                );
        long confirmedRequests = requestQuery.countRequestsByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED);
        long views = eventStatsAdapter.getViews(event.getId());
        long comments = commentQuery.countCommentsByEventIdAndState(event.getId(), CommentState.PUBLISHED);

        return EventMapper.toEventShortDto(event, confirmedRequests, views, comments);
    }

    private List<EventShortDto> getEventShortDtos(List<Event> events, List<Long> eventsIds) {
        Map<Long, Long> confirmedRequestsMap = requestQuery
                .countRequestsByEventIdsAndStatus(eventsIds, RequestStatus.CONFIRMED);
        Map<Long, Long> viewsMap = eventStatsAdapter
                .getViews(eventsIds);
        Map<Long, Long> commentsMap = commentQuery.countCommentsByEventIdsAndState(eventsIds, CommentState.PUBLISHED);

        return EventMapper.toListEventShortDto(events, confirmedRequestsMap, viewsMap, commentsMap);
    }

    private List<Long> getEventsIds(List<Event> events) {
        return events.stream()
                .map(Event::getId)
                .toList();
    }
}
