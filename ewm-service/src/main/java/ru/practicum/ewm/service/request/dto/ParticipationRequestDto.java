package ru.practicum.ewm.service.request.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import ru.practicum.ewm.service.request.model.RequestStatus;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
public class ParticipationRequestDto {
    private Long id;

    private LocalDateTime created;

    @JsonProperty("event")
    private Long eventId;

    @JsonProperty("requester")
    private Long requesterId;

    private RequestStatus status;
}
