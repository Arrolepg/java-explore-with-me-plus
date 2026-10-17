package ru.practicum.ewm.service.compilation.utility;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompilationPaginationParams {

    @PositiveOrZero(message = "Параметр from не может быть отрицательным")
    private Integer from = 0;

    @Min(value = 1, message = "Параметр size должен быть больше нуля")
    private Integer size = 10;
}



