package ru.practicum.ewm.service.comment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.service.comment.dto.request.CommentAdminUpdateDto;
import ru.practicum.ewm.service.comment.dto.response.CommentResponseDto;
import ru.practicum.ewm.service.comment.model.AdminCommentStateAction;
import ru.practicum.ewm.service.comment.model.Comment;
import ru.practicum.ewm.service.comment.model.CommentState;
import ru.practicum.ewm.service.comment.repository.CommentRepository;
import ru.practicum.ewm.service.comment.repository.CommentSearchRepository;
import ru.practicum.ewm.service.comment.repository.CommentSpecifications;
import ru.practicum.ewm.service.comment.utility.AdminCommentSearchFilter;
import ru.practicum.ewm.service.comment.utility.CommentMapper;
import ru.practicum.ewm.service.comment.utility.CommentPaginationParams;
import ru.practicum.ewm.service.event.model.*;

import ru.practicum.ewm.service.exception.ConflictException;
import ru.practicum.ewm.service.exception.NotFoundException;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {
    private static final Sort SORT_BY_ID_ASC = Sort.by(Sort.Direction.ASC, Event_.ID);
    private final CommentRepository commentRepository;
    private final CommentSearchRepository commentSearchRepository;

    @Override
    public List<CommentResponseDto> findAllByAdmin(AdminCommentSearchFilter searchFilter, CommentPaginationParams commentPaginationParams) {

        List<Comment> comments = commentSearchRepository.findAllWithFetch(CommentSpecifications.forAdminSearch(searchFilter),
                commentPaginationParams);
        if (comments.isEmpty()) {
            return List.of();
        }


        return comments.stream()
                .map(comment -> CommentMapper.toCommentResponseDto(comment))
                .toList();
    }

    @Override
    @Transactional
    public CommentResponseDto updateByAdmin(Long commentId, CommentAdminUpdateDto commentDto) {
        Comment comment = commentRepository.findById(commentId).orElseThrow(
                () -> new NotFoundException("Комментарий с id = " + commentId + " не найден")
        );

        checkAdminStateAction(comment, commentDto);
        applyAdminStateAction(comment, commentDto.getAdminCommentStateAction());

        return CommentMapper.toCommentResponseDto(comment);
    }

    private void checkAdminStateAction(Comment comment, CommentAdminUpdateDto commentDto) {
        AdminCommentStateAction adminCommentStateAction = commentDto.getAdminCommentStateAction();
        if (adminCommentStateAction == null) {
            return;
        }

        if (adminCommentStateAction == AdminCommentStateAction.PUBLISH_COMMENT) {
            if (comment.getState() != CommentState.PENDING) {
                throw new ConflictException("Нельзя опубликовать комментарий, так как он не в состоянии "
                        + CommentState.PENDING + ": " + comment.getState());
            }
        } else if (adminCommentStateAction == AdminCommentStateAction.REJECT_COMMENT && comment.getState() == CommentState.PUBLISHED) {
            throw new ConflictException("Нельзя отклонить комментарий, так как он уже опубликован");
        }
    }

    private void applyAdminStateAction(Comment comment, AdminCommentStateAction commentStateAction) {
        if (commentStateAction == null) {
            return;
        }
        switch (commentStateAction) {
            case PUBLISH_COMMENT -> comment.setState(CommentState.PUBLISHED);
            case REJECT_COMMENT -> comment.setState(CommentState.REJECTED);
        }
    }

    private Pageable toPageable(CommentPaginationParams commentPaginationParams) {
        return PageRequest.of(
                commentPaginationParams.from() / commentPaginationParams.size(),
                commentPaginationParams.size(),
                SORT_BY_ID_ASC);
    }

}
