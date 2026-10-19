package ru.practicum.ewm.service.event.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class EventLocationUpdateDto {
    @Min(value = -90, message = "Широта должна быть не менее -90 граусов")
    @Max(value = 90, message = "Широта должна быть не более 90 градусов")
    private Float lat;

    @Min(value = -180, message = "Долгота должна быть не менее -180 градусов")
    @Max(value = 180, message = "Долгота должна быть не более 180 градусов")
    private Float lon;
}
