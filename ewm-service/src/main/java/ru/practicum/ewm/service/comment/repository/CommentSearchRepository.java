package ru.practicum.ewm.service.comment.repository;

import ru.practicum.ewm.service.comment.model.Comment;
import ru.practicum.ewm.service.comment.utility.CommentPaginationParams;

import java.util.List;

public interface CommentSearchRepository {
    List<Comment> findAllByAuthorId(Long userId, CommentPaginationParams params);
}
