package ru.practicum.ewm.service.compilation.utility;

import jakarta.validation.constraints.Min;
import lombok.Builder;

@Builder
public record CompilationPaginationParams(
        @Min(value = 0, message = "Параметр from не может быть отрицательным")
        Integer from,

        @Min(value = 1, message = "Параметр size должен быть больше нуля")
        Integer size
) {

    public CompilationPaginationParams {
        if (from == null) from = 0;
        if (size == null) size = 10;
    }
}



