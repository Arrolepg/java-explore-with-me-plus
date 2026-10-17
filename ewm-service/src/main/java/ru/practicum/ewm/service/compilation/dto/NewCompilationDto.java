package ru.practicum.ewm.service.compilation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class NewCompilationDto {
    private List<Long> events;
    private Boolean pinned;

    @NotBlank(message = "Заголовок подборки должен быть указан")
    @Size(min = 1, max = 50)
    private String title;
}
