package ru.practicum.ewm.service.event.utility;

import ru.practicum.ewm.service.event.model.EventState;

import java.time.LocalDateTime;
import java.util.List;

public record AdminEventSearchRequest(
        List<Long> users,
        List<EventState> states,
        List<Long> categories,
        LocalDateTime rangeStart,
        LocalDateTime rangeEnd,
        Integer from,
        Integer size
) {
}
