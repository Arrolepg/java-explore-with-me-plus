package ru.practicum.ewm.service.comment.utility;

import ru.practicum.ewm.service.comment.model.CommentStateFilter;

import java.util.List;

public record AdminCommentSearchFilter(
        List<Long> users,
        List<Long> events,
        CommentStateFilter state
) {
    public AdminCommentSearchFilter {
        if (state == null) {
            state = CommentStateFilter.ALL;
        }
    }
}
