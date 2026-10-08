package ru.practicum.ewm.service.event.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import ru.practicum.common.util.PatternDataTime;
import ru.practicum.ewm.service.validation.TwoHoursFromNow;

import java.time.LocalDateTime;

@Data
public class NewEventDto {
    @NotBlank(message = "Краткое описание события должно быть указано")
    @Size(min = 20, max = 2000, message = "Минимальная длина краткого описания события - 20 символов. " +
            "Максимальная длина краткого описания события - 2000 символов")
    private String annotation;

    @NotNull(message = "Id категории события должно быть указано")
    @JsonProperty("category")
    private Long categoryId;

    @NotBlank(message = "Полное описание события должно быть указано")
    @Size(min = 20, max = 7000, message = "Минимальная длина полного описания события - 20 символов. " +
            "Максимальная длина полного описания события - 7000 символов")
    private String description;

    @NotNull(message = "Дата и время на которые намечено событие должны быть указаны")
    @JsonFormat(pattern = PatternDataTime.PATTERN)
    @TwoHoursFromNow
    private LocalDateTime eventDate;

    @NotNull(message = "Локация проведения события должна быть указана")
    @Valid
    private EventLocationCreateDto location;

    private Boolean paid = false;

    private Integer participantLimit = 0;

    private Boolean requestModeration = true;

    @NotBlank(message = "Заголовок события должен быть указан")
    @Size(min = 3, max = 120, message = "Минимальная длина заголовка события - 3 символа. " +
            "Максимальная длина заголовка события - 120 символов")
    private String title;
}
