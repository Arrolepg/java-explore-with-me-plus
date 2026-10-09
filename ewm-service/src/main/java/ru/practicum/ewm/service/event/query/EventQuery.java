package ru.practicum.ewm.service.event.query;

import ru.practicum.ewm.service.event.model.Event;

public interface EventQuery {
    Event findEvent(Long eventId);
}
