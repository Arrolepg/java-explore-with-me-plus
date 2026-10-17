package ru.practicum.ewm.service.event.query;

import ru.practicum.ewm.service.event.model.Event;

import java.util.List;

public interface EventQuery {
    Event findEvent(Long eventId);

    boolean existsByCategoryId(Long categoryId);

    List<Event> findAllById(List<Long> eventsIds);
}
