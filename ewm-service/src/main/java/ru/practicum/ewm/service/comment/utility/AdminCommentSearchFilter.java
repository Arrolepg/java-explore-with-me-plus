package ru.practicum.ewm.service.comment.utility;

import jakarta.validation.constraints.Pattern;

import java.util.List;

public record AdminCommentSearchFilter(
        List<Long> users,
        List<Long> events,
        @Pattern(regexp = "^(PENDING|PUBLISHED|REJECTED|ALL)$", message = "Неизвестный статус комментария")
        String state
) {
    public AdminCommentSearchFilter {
        if (state == null) state = "ALL";
    }
}
