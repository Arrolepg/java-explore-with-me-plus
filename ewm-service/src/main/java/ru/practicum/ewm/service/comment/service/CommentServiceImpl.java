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
import ru.practicum.ewm.service.comment.model.Comment_;
import ru.practicum.ewm.service.comment.repository.CommentRepository;
import ru.practicum.ewm.service.comment.repository.CommentSpecifications;
import ru.practicum.ewm.service.comment.utility.AdminCommentSearchFilter;
import ru.practicum.ewm.service.comment.utility.CommentMapper;
import ru.practicum.ewm.service.comment.utility.CommentPaginationParams;
import ru.practicum.ewm.service.event.dto.response.EventShortDto;

import ru.practicum.ewm.service.exception.ConflictException;
import ru.practicum.ewm.service.exception.NotFoundException;

import ru.practicum.ewm.service.comment.dto.request.CommentAuthorRequestDto;
import ru.practicum.ewm.service.comment.dto.response.CommentAuthorResponseDto;
import ru.practicum.ewm.service.event.model.Event;
import ru.practicum.ewm.service.event.query.EventQuery;
import ru.practicum.ewm.service.request.model.Request;
import ru.practicum.ewm.service.request.model.RequestStatus;
import ru.practicum.ewm.service.request.query.RequestQuery;
import ru.practicum.ewm.service.user.model.User;
import ru.practicum.ewm.service.user.query.UserQuery;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepository;
    private final UserQuery userQuery;
    private final EventQuery eventQuery;
    private final RequestQuery requestQuery;
    private static final Sort SORT_BY_ID_ASC = Sort.by(Sort.Direction.ASC, Comment_.ID);


    @Override
    public List<CommentAuthorResponseDto> findAllByAuthor(Long userId, CommentPaginationParams params) {
        userQuery.findUser(userId);
        List<Comment> comments = commentRepository.findAllByAuthorId(userId, params);

        if (comments.isEmpty()) {
            return List.of();
        }

        List<CommentAuthorResponseDto> commentsDto = CommentMapper.toListCommentAuthorResponseDto(comments);
        log.info("Получены комментарии пользователя: userId = {}, count = {}", userId, commentsDto.size());

        return commentsDto;
    }

    @Override
    public CommentAuthorResponseDto findById(Long userId, Long commentId) {
        userQuery.findUser(userId);
        Comment comment = getCommentByAuthor(userId, commentId);

        log.info("Получен комментарий пользователя: userId = {}, commentId = {}", userId, commentId);

        return CommentMapper.toCommentAuthorResponseDto(comment);
    }

    @Override
    @Transactional
    public CommentAuthorResponseDto create(Long userId, CommentAuthorRequestDto commentRequestDto, Long eventId) {
        LocalDateTime timeCreate = LocalDateTime.now();
        User user = userQuery.findUser(userId);
        Event event = eventQuery.findEvent(eventId);

        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Инициатор события не может добавить комментарий к своёму событию");
        }

        if (commentRepository.existsByAuthorIdAndEventId(userId, eventId)) {
            throw new ConflictException("Комментарий к событию id = " + eventId + " был создан ранее");
        }

        Request request = requestQuery.findByRequesterIdAndEventId(userId, eventId);

        if (request.getStatus() != RequestStatus.CONFIRMED) {
            throw new ConflictException("Заявка id = " + request.getId() + " пользователя id = " + userId +
                    " на участие в событии id = " + eventId + " не была одобрена");
        }

        if (timeCreate.isBefore(event.getEventDate())) {
            throw new ConflictException("Событие id = " + eventId + " ещё не прошло");
        }

        Comment comment = Comment.builder()
                .event(event)
                .author(user)
                .text(commentRequestDto.getText())
                .build();

        commentRepository.save(comment);
        log.info("Комментарий пользователя сохранен: userId = {}, commentId = {}", userId, comment.getId());

        return CommentMapper.toCommentAuthorResponseDto(comment);
    }

    @Override
    @Transactional
    public CommentAuthorResponseDto update(Long userId, CommentAuthorRequestDto commentRequestDto, Long commentId) {
        userQuery.findUser(userId);
        Comment comment = getCommentByAuthor(userId, commentId);

        comment.setText(commentRequestDto.getText());
        comment.setState(CommentState.PENDING);
        comment.setPublishedOn(null);

        commentRepository.save(comment);

        log.info("Комментарий пользователя изменен: commentId = {}, userId = {}", commentId, userId);
        return CommentMapper.toCommentAuthorResponseDto(comment);
    }

    @Override
    @Transactional
    public void delete(Long userId, Long commentId) {
        userQuery.findUser(userId);
        Comment comment = getCommentByAuthor(userId, commentId);

        commentRepository.delete(comment);
        log.info("Удален комментарий с id = {}", commentId);
    }

    private Comment getCommentByAuthor(Long userId, Long commentId) {
        return commentRepository.findByIdAndAuthorId(commentId, userId)
                .orElseThrow(() -> new NotFoundException("Комментарий id = " + commentId +
                        " пользователя id = " + userId + " не найден"));
    }

    @Override
    public List<CommentResponseDto> findAllByAdmin(AdminCommentSearchFilter searchFilter, CommentPaginationParams commentPaginationParams) {

        List<Comment> comments = commentRepository.findAllWithFetch(
                CommentSpecifications.forAdminSearch(searchFilter),
                toPageable(commentPaginationParams)
        );

        if (comments.isEmpty()) {
            return List.of();
        }
        List<Long> eventIds = comments.stream()
                .map(comment -> comment.getEvent().getId())
                .distinct()
                .toList();

        List<EventShortDto> events = eventQuery.getEventsDtoByIds(eventIds);
        Map<Long, EventShortDto> eventsMap = events.stream()
                .collect(Collectors.toMap(EventShortDto::getId, dto -> dto));
        return CommentMapper.toListCommentResponseDto(comments, eventsMap);
    }

    @Override
    @Transactional
    public CommentResponseDto updateByAdmin(Long commentId, CommentAdminUpdateDto commentDto) {
        Comment comment = commentRepository.findById(commentId).orElseThrow(
                () -> new NotFoundException("Комментарий с id = " + commentId + " не найден")
        );

        checkAdminStateAction(comment, commentDto);
        applyAdminStateAction(comment, commentDto.getAdminCommentStateAction());

        EventShortDto event = eventQuery.getEventDtoById(comment.getEvent().getId());

        return CommentMapper.toCommentResponseDto(comment, event);
    }

    private void checkAdminStateAction(Comment comment, CommentAdminUpdateDto commentDto) {
        AdminCommentStateAction adminCommentStateAction = commentDto.getAdminCommentStateAction();

        if (adminCommentStateAction == AdminCommentStateAction.PUBLISH_COMMENT) {
            if (comment.getState() != CommentState.PENDING) {
                throw new ConflictException("Нельзя опубликовать комментарий, так как он не в состоянии "
                        + CommentState.PENDING + ": " + comment.getState());
            }
        } else if (adminCommentStateAction == AdminCommentStateAction.REJECT_COMMENT
                && comment.getState() == CommentState.PUBLISHED) {
            throw new ConflictException("Нельзя отклонить комментарий, так как он уже опубликован");
        }
    }

    private void applyAdminStateAction(Comment comment, AdminCommentStateAction commentStateAction) {
        switch (commentStateAction) {
            case PUBLISH_COMMENT -> {
                comment.setState(CommentState.PUBLISHED);
                comment.setPublishedOn(LocalDateTime.now());
            }
            case REJECT_COMMENT -> comment.setState(CommentState.REJECTED);
        }
    }

    private Pageable toPageable(CommentPaginationParams commentPaginationParams) {
        if (commentPaginationParams.sort() == null) {
            return PageRequest.of(
                    commentPaginationParams.from() / commentPaginationParams.size(),
                    commentPaginationParams.size(),
                    SORT_BY_ID_ASC);
        }

        return PageRequest.of(
                commentPaginationParams.from() / commentPaginationParams.size(),
                commentPaginationParams.size(),
                Sort.by(Sort.Direction.DESC, Comment_.CREATED_ON)
                        .and(SORT_BY_ID_ASC));
    }
}
