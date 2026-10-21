package ru.practicum.ewm.service.comment.service;

import ru.practicum.ewm.service.comment.dto.request.CommentAuthorRequestDto;
import ru.practicum.ewm.service.comment.dto.response.CommentAuthorResponseDto;
import ru.practicum.ewm.service.comment.utility.CommentPaginationParams;

import java.util.List;

public interface CommentService {
    List<CommentAuthorResponseDto> findAllByAuthor(Long userId, CommentPaginationParams params);

    CommentAuthorResponseDto findById(Long userId, Long commentId);

    CommentAuthorResponseDto create(Long userId, CommentAuthorRequestDto commentRequestDto, Long eventId);

    CommentAuthorResponseDto update(Long userId, CommentAuthorRequestDto commentRequestDto, Long commentId);

    void delete(Long userId, Long commentId);
}
