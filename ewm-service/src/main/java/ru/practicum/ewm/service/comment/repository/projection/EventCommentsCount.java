package ru.practicum.ewm.service.comment.repository.projection;

public record EventCommentsCount(
        Long eventId,
        Long commentsCount
) {}
