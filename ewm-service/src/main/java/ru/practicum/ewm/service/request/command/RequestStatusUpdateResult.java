package ru.practicum.ewm.service.request.command;

import ru.practicum.ewm.service.request.dto.ParticipationRequestDto;

import java.util.List;

public record RequestStatusUpdateResult(
        List<ParticipationRequestDto> confirmedRequests,
        List<ParticipationRequestDto> rejectedRequests
) {}
