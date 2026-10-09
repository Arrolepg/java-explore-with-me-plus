package ru.practicum.ewm.service.request.command;

import ru.practicum.ewm.service.request.model.RequestStatus;

import java.util.List;

public record RequestStatusUpdateCommand(
        Long eventId,
        List<Long> requestsIds,
        RequestStatus requestStatus,
        Integer participantLimit
) {}
