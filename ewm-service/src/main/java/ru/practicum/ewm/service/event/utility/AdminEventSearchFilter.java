package ru.practicum.ewm.service.event.utility;

import org.springframework.format.annotation.DateTimeFormat;
import ru.practicum.common.util.PatternDataTime;
import ru.practicum.ewm.service.event.model.EventState;

import java.time.LocalDateTime;
import java.util.List;

public record AdminEventSearchFilter(
        List<Long> users,
        List<EventState> states,
        List<Long> categories,

        @DateTimeFormat(pattern = PatternDataTime.PATTERN)
        LocalDateTime rangeStart,

        @DateTimeFormat(pattern = PatternDataTime.PATTERN)
        LocalDateTime rangeEnd
) {
}
