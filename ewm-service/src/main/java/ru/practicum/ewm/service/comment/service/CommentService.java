package ru.practicum.ewm.service.comment.service;

import ru.practicum.ewm.service.comment.dto.request.CommentAdminUpdateDto;
import ru.practicum.ewm.service.comment.dto.response.CommentResponseDto;
import ru.practicum.ewm.service.comment.utility.AdminCommentSearchFilter;
import ru.practicum.ewm.service.comment.dto.request.CommentAuthorRequestDto;
import ru.practicum.ewm.service.comment.dto.response.CommentAuthorResponseDto;
import ru.practicum.ewm.service.comment.utility.CommentPaginationParams;

import java.util.List;

public interface CommentService {
    List<CommentResponseDto> findAllByAdmin(AdminCommentSearchFilter searchFilter, CommentPaginationParams commentPaginationParams);

    CommentResponseDto updateByAdmin(Long commentId, CommentAdminUpdateDto commentDto);
    List<CommentAuthorResponseDto> findAllByAuthor(Long userId, CommentPaginationParams params);

    CommentAuthorResponseDto findById(Long userId, Long commentId);

    CommentAuthorResponseDto create(Long userId, CommentAuthorRequestDto commentRequestDto, Long eventId);

    CommentAuthorResponseDto update(Long userId, CommentAuthorRequestDto commentRequestDto, Long commentId);

    void delete(Long userId, Long commentId);
}
