package ru.practicum.ewm.service.event.model;

import jakarta.persistence.Embeddable;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class EventLocation {
    private Float lat;

    private Float lon;
}
