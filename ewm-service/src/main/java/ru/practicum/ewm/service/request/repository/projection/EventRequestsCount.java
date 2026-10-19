package ru.practicum.ewm.service.request.repository.projection;

public record EventRequestsCount(
        Long eventId,
        Long count
) {}
