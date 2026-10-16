package ru.practicum.ewm.service.event.utility;

import org.springframework.format.annotation.DateTimeFormat;
import ru.practicum.common.util.PatternDataTime;

import java.time.LocalDateTime;
import java.util.List;

public record PublicEventSearchFilter(
        String text,

        List<Long> categories,

        Boolean paid,

        @DateTimeFormat(pattern = PatternDataTime.PATTERN)
        LocalDateTime rangeStart,

        @DateTimeFormat(pattern = PatternDataTime.PATTERN)
        LocalDateTime rangeEnd,

        Boolean onlyAvailable
) {
        public PublicEventSearchFilter {
                if (onlyAvailable == null) {
                        onlyAvailable = false;
                }
        }
}
