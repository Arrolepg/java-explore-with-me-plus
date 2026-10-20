package ru.practicum.ewm.service.comment.utility;

import java.util.List;

public record AdminCommentSearchFilter(
        List<Long> users,
        List<Long> events,
        String state
) {
    public AdminCommentSearchFilter {
        if (state == null) state = "ALL";
    }
}
