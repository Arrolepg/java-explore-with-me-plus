package ru.practicum.ewm.service.comment.utility;

import jakarta.validation.constraints.Min;
import ru.practicum.ewm.service.comment.model.CommentSearchSort;

public record CommentPaginationParams(
        @Min(value = 0, message = "Параметр from не может быть отрицательным")
        Integer from,

        @Min(value = 1, message = "Параметр size должен быть больше нуля")
        Integer size,

        CommentSearchSort sort
) {
    public CommentPaginationParams {
        if (from == null) from = 0;
        if (size == null) size = 10;
    }
}
