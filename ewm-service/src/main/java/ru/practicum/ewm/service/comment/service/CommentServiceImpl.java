package ru.practicum.ewm.service.comment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.service.comment.dto.request.CommentAuthorRequestDto;
import ru.practicum.ewm.service.comment.dto.response.CommentAuthorResponseDto;
import ru.practicum.ewm.service.comment.model.Comment;
import ru.practicum.ewm.service.comment.model.CommentState;
import ru.practicum.ewm.service.comment.repository.CommentRepository;
import ru.practicum.ewm.service.comment.utility.CommentMapper;
import ru.practicum.ewm.service.comment.utility.CommentPaginationParams;
import ru.practicum.ewm.service.event.model.Event;
import ru.practicum.ewm.service.event.query.EventQuery;
import ru.practicum.ewm.service.exception.ConflictException;
import ru.practicum.ewm.service.exception.NotFoundException;
import ru.practicum.ewm.service.request.model.Request;
import ru.practicum.ewm.service.request.model.RequestStatus;
import ru.practicum.ewm.service.request.query.RequestQuery;
import ru.practicum.ewm.service.user.model.User;
import ru.practicum.ewm.service.user.query.UserQuery;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepository;
    private final UserQuery userQuery;
    private final EventQuery eventQuery;
    private final RequestQuery requestQuery;

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
        LocalDateTime timeCreate = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
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
                                    .createdOn(timeCreate)
                                    .state(CommentState.PENDING).build();

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

}
