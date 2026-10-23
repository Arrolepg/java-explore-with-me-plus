package ru.practicum.ewm.service.comment.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.service.comment.dto.request.CommentAuthorRequestDto;
import ru.practicum.ewm.service.comment.dto.response.CommentAuthorResponseDto;
import ru.practicum.ewm.service.comment.model.Comment;
import ru.practicum.ewm.service.comment.model.CommentState;
import ru.practicum.ewm.service.comment.repository.CommentRepository;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplUnitTest {
    private static final Long USER_ID = 2L;
    private static final Long EVENT_ID = 3L;
    private static final Long COMMENT_ID = 11L;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private UserQuery userQuery;

    @Mock
    private EventQuery eventQuery;

    @Mock
    private RequestQuery requestQuery;

    @InjectMocks
    private CommentServiceImpl commentService;

    private User user;
    private Event event;
    private Comment comment;
    private CommentAuthorRequestDto requestDto;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(USER_ID);

        User initiator = new User();
        initiator.setId(100L);

        event = Event.builder()
                .id(EVENT_ID)
                .initiator(initiator)
                .eventDate(LocalDateTime.now().minusDays(1))
                .build();

        comment = Comment.builder()
                .id(COMMENT_ID)
                .author(user)
                .event(event)
                .text("comment")
                .createdOn(LocalDateTime.of(2026, 10, 1, 12, 0))
                .state(CommentState.PENDING)
                .build();

        requestDto = new CommentAuthorRequestDto();
        requestDto.setText("new text");

        when(userQuery.findUser(USER_ID)).thenReturn(user);
    }

    @Test
    void findAllByAuthorReturnsComments() {
        CommentPaginationParams params = new CommentPaginationParams(0, 10, null);

        when(commentRepository.findAllByAuthorId(USER_ID, params))
                .thenReturn(List.of(comment));

        List<CommentAuthorResponseDto> result = commentService.findAllByAuthor(USER_ID, params);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(COMMENT_ID);
        assertThat(result.getFirst().getEventId()).isEqualTo(EVENT_ID);
        assertThat(result.getFirst().getText()).isEqualTo("comment");

        verify(commentRepository).findAllByAuthorId(USER_ID, params);
    }

    @Test
    void findAllByAuthorReturnsEmptyList() {
        CommentPaginationParams params = new CommentPaginationParams(0, 10, null);

        when(commentRepository.findAllByAuthorId(USER_ID, params))
                .thenReturn(List.of());

        List<CommentAuthorResponseDto> result = commentService.findAllByAuthor(USER_ID, params);

        assertThat(result).isEmpty();

        verify(commentRepository).findAllByAuthorId(USER_ID, params);
    }

    @Test
    void findByIdReturnsComment() {
        when(commentRepository.findByIdAndAuthorId(COMMENT_ID, USER_ID))
                .thenReturn(Optional.of(comment));

        CommentAuthorResponseDto result = commentService.findById(USER_ID, COMMENT_ID);

        assertThat(result.getId()).isEqualTo(COMMENT_ID);
        assertThat(result.getEventId()).isEqualTo(EVENT_ID);
        assertThat(result.getText()).isEqualTo("comment");
        assertThat(result.getState()).isEqualTo(CommentState.PENDING);

        verify(commentRepository).findByIdAndAuthorId(COMMENT_ID, USER_ID);
    }

    @Test
    void findByIdThrowsWhenCommentNotFound() {
        when(commentRepository.findByIdAndAuthorId(COMMENT_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> commentService.findById(USER_ID, COMMENT_ID)
        ).isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateChangesTextAndReturnsCommentToPending() {
        comment.setState(CommentState.PUBLISHED);
        comment.setPublishedOn(LocalDateTime.of(2026, 10, 3, 12, 0));

        when(commentRepository.findByIdAndAuthorId(COMMENT_ID, USER_ID))
                .thenReturn(Optional.of(comment));

        CommentAuthorResponseDto result = commentService.update(USER_ID, requestDto, COMMENT_ID);

        assertThat(result.getId()).isEqualTo(COMMENT_ID);
        assertThat(result.getText()).isEqualTo("new text");
        assertThat(result.getState()).isEqualTo(CommentState.PENDING);
        assertThat(result.getPublishedOn()).isNull();

        assertThat(comment.getText()).isEqualTo("new text");
        assertThat(comment.getState()).isEqualTo(CommentState.PENDING);

        verify(commentRepository).save(comment);
    }

    @Test
    void updateThrowsWhenCommentNotFound() {
        when(commentRepository.findByIdAndAuthorId(COMMENT_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> commentService.update(USER_ID, requestDto, COMMENT_ID))
                .isInstanceOf(NotFoundException.class);

        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void deleteRemovesComment() {
        when(commentRepository.findByIdAndAuthorId(COMMENT_ID, USER_ID))
                .thenReturn(Optional.of(comment));

        commentService.delete(USER_ID, COMMENT_ID);

        verify(commentRepository).findByIdAndAuthorId(COMMENT_ID, USER_ID);
        verify(commentRepository).delete(comment);
    }

    @Test
    void deleteThrowsWhenCommentNotFound() {
        when(commentRepository.findByIdAndAuthorId(COMMENT_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> commentService.delete(USER_ID, COMMENT_ID))
                .isInstanceOf(NotFoundException.class);

        verify(commentRepository, never()).delete(any(Comment.class));
    }

    @Test
    void createReturnsPendingComment() {
        prepareCreate(RequestStatus.CONFIRMED);

        CommentAuthorResponseDto result = commentService.create(USER_ID, requestDto, EVENT_ID);

        assertThat(result.getEventId()).isEqualTo(EVENT_ID);
        assertThat(result.getText()).isEqualTo("new text");
        assertThat(result.getState()).isEqualTo(CommentState.PENDING);
        assertThat(result.getCreatedOn()).isNotNull();
        assertThat(result.getPublishedOn()).isNull();

        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    void createThrowsWhenUserIsInitiator() {
        event.setInitiator(user);
        when(eventQuery.findEvent(EVENT_ID)).thenReturn(event);

        assertThatThrownBy(
                () -> commentService.create(USER_ID, requestDto, EVENT_ID))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Инициатор события");

        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void createThrowsWhenCommentAlreadyExists() {
        when(eventQuery.findEvent(EVENT_ID)).thenReturn(event);
        when(commentRepository.existsByAuthorIdAndEventId(USER_ID, EVENT_ID))
                .thenReturn(true);

        assertThatThrownBy(
                () -> commentService.create(USER_ID, requestDto, EVENT_ID))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("был создан ранее");

        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void createThrowsWhenRequestNotConfirmed() {
        prepareCreate(RequestStatus.PENDING);

        assertThatThrownBy(
                () -> commentService.create(USER_ID, requestDto, EVENT_ID))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("не была одобрена");

        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void createThrowsWhenEventIsInFuture() {
        event.setEventDate(LocalDateTime.now().plusDays(1));
        prepareCreate(RequestStatus.CONFIRMED);

        assertThatThrownBy(
                () -> commentService.create(USER_ID, requestDto, EVENT_ID))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("ещё не прошло");

        verify(commentRepository, never()).save(any(Comment.class));
    }

    private void prepareCreate(RequestStatus status) {
        when(eventQuery.findEvent(EVENT_ID)).thenReturn(event);
        when(commentRepository.existsByAuthorIdAndEventId(USER_ID, EVENT_ID))
                .thenReturn(false);

        Request participation = Request.builder()
                .id(20L)
                .requester(user)
                .event(event)
                .status(status)
                .build();

        when(requestQuery.findByRequesterIdAndEventId(USER_ID, EVENT_ID))
                .thenReturn(participation);
    }
}