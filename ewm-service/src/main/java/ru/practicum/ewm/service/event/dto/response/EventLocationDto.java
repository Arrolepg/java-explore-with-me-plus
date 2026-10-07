package ru.practicum.ewm.service.event.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class EventLocationDto {
    private Float lat;

    private Float lon;
}
