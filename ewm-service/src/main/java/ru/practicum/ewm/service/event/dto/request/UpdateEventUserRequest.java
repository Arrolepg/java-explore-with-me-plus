package ru.practicum.ewm.service.event.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Data;
import ru.practicum.common.util.PatternDataTime;
import ru.practicum.ewm.service.event.model.UserEventStateAction;
import ru.practicum.ewm.service.validation.TwoHoursFromNow;

import java.time.LocalDateTime;

@Data
public class UpdateEventUserRequest {
    @Size(min = 20, max = 2000, message = "Минимальная длина краткого описания события - 20 символов. " +
            "Максимальная длина краткого описания события - 2000 символов")
    private String annotation;

    @JsonProperty("category")
    private Long categoryId;

    @Size(min = 20, max = 7000, message = "Минимальная длина полного описания события - 20 символов. " +
            "Максимальная длина полного описания события - 7000 символов")
    private String description;

    @JsonFormat(pattern = PatternDataTime.PATTERN)
    @TwoHoursFromNow
    private LocalDateTime eventDate;

    @Valid
    private EventLocationUpdateDto location;

    private Boolean paid;

    private Integer participantLimit;

    private Boolean requestModeration;

    private UserEventStateAction stateAction;

    @Size(min = 3, max = 120, message = "Минимальная длина заголовка события - 3 символа. " +
            "Максимальная длина заголовка события - 120 символов")
    private String title;
}
