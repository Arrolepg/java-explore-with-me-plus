package ru.practicum.ewm.service.category.utility;

import jakarta.validation.constraints.Min;

public record CategoryPaginationParams(
        @Min(value = 0, message = "Параметр from не может быть отрицательным")
        Integer from,

        @Min(value = 1, message = "Параметр size должен быть больше нуля")
        Integer size
) {
    public CategoryPaginationParams {
        if (from == null) from = 0;
        if (size == null) size = 10;
    }
}
