package ru.practicum.ewm.service.common;

import jakarta.validation.constraints.Min;
import ru.practicum.ewm.service.event.model.EventSearchSort;

public record PaginationParams(
        @Min(value = 0, message = "Параметр from не может быть отрицательным")
        Integer from,

        @Min(value = 1, message = "Параметр size должен быть больше нуля")
        Integer size,

        EventSearchSort sort
) {
        public PaginationParams {
                if (from == null) from = 0;
                if (size == null) size = 10;
        }
}
