package ru.practicum.ewm.service.comment.query;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.service.comment.dto.response.CommentEventResponseDto;
import ru.practicum.ewm.service.comment.model.Comment;
import ru.practicum.ewm.service.comment.model.CommentState;
import ru.practicum.ewm.service.comment.repository.CommentRepository;
import ru.practicum.ewm.service.comment.repository.projection.EventCommentsCount;
import ru.practicum.ewm.service.event.model.Event;
import ru.practicum.ewm.service.user.model.User;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CommentQueryImplUnitTest {
    private static final Long EVENT_ID_1 = 1L;
    private static final Long EVENT_ID_2 = 2L;
    private static final Long COMMENT_ID_1 = 10L;
    private static final Long COMMENT_ID_2 = 11L;
    private static final Long COMMENT_ID_3 = 12L;
    private static final Long AUTHOR_ID_1 = 100L;
    private static final Long AUTHOR_ID_2 = 101L;
    private static final String AUTHOR_NAME = "Name";
    private static final String TEXT = "text";

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private CommentQueryImpl commentQuery;

    @Test
    void testCountCommentsByEventIdsAndStateReturnMap() {
        List<Long> ids = List.of(EVENT_ID_1, EVENT_ID_2);
        when(commentRepository.countCommentsByEventIdsAndState(ids, CommentState.PUBLISHED))
                .thenReturn(List.of(
                        new EventCommentsCount(EVENT_ID_1, 3L),
                        new EventCommentsCount(EVENT_ID_2, 5L)
                ));
        Map<Long, Long> result = commentQuery.countCommentsByEventIdsAndState(ids, CommentState.PUBLISHED);

        assertThat(result)
                .hasSize(2)
                .containsEntry(EVENT_ID_1, 3L)
                .containsEntry(EVENT_ID_2, 5L);
        verify(commentRepository).countCommentsByEventIdsAndState(ids, CommentState.PUBLISHED);
        verifyNoMoreInteractions(commentRepository);
    }

    @Test
    void testCountCommentsByEventIdsAndStateReturnEmptyMap() {
        List<Long> ids = List.of(EVENT_ID_1, EVENT_ID_2);
        when(commentRepository.countCommentsByEventIdsAndState(ids, CommentState.PUBLISHED))
                .thenReturn(List.of());
        Map<Long, Long> result = commentQuery.countCommentsByEventIdsAndState(ids, CommentState.PUBLISHED);

        assertThat(result).isEmpty();
        verify(commentRepository).countCommentsByEventIdsAndState(ids, CommentState.PUBLISHED);
        verifyNoMoreInteractions(commentRepository);
    }

    @Test
    void testCountCommentsByEventIdsAndStatePassesCommentState() {
        List<Long> ids = List.of(EVENT_ID_1, EVENT_ID_2);
        when(commentRepository.countCommentsByEventIdsAndState(ids, CommentState.PENDING))
                .thenReturn(List.of(
                        new EventCommentsCount(EVENT_ID_1, 3L),
                        new EventCommentsCount(EVENT_ID_2, 5L)
                ));
        Map<Long, Long> result = commentQuery.countCommentsByEventIdsAndState(ids, CommentState.PENDING);

        assertThat(result)
                .hasSize(2)
                .containsEntry(EVENT_ID_1, 3L)
                .containsEntry(EVENT_ID_2, 5L);
        verify(commentRepository).countCommentsByEventIdsAndState(ids, CommentState.PENDING);
        verify(commentRepository, never())
                .countCommentsByEventIdsAndState(anyList(), eq(CommentState.PUBLISHED));
    }

    @Test
    void testFindCommentsWithAuthorByEventIdAndStateReturnList() {
        Comment comment1 = createComment(COMMENT_ID_1, EVENT_ID_1, AUTHOR_ID_1);
        Comment comment2 = createComment(COMMENT_ID_2, EVENT_ID_1, AUTHOR_ID_2);

        when(commentRepository.findCommentsByEventIdAndStateWithAuthorFetch(EVENT_ID_1, CommentState.PUBLISHED))
                .thenReturn(List.of(comment1, comment2));
        List<CommentEventResponseDto> result = commentQuery.findCommentsWithAuthorByEventIdAndState(EVENT_ID_1,
                CommentState.PUBLISHED);

        assertThat(result)
                .hasSize(2)
                .extracting(CommentEventResponseDto::getId)
                .containsExactly(COMMENT_ID_1, COMMENT_ID_2);
        verify(commentRepository).findCommentsByEventIdAndStateWithAuthorFetch(EVENT_ID_1, CommentState.PUBLISHED);
        verifyNoMoreInteractions(commentRepository);
    }

    @Test
    void testFindCommentsWithAuthorByEventIdAndStateReturnEmptyList() {
        when(commentRepository.findCommentsByEventIdAndStateWithAuthorFetch(EVENT_ID_1, CommentState.PUBLISHED))
                .thenReturn(List.of());
        List<CommentEventResponseDto> result = commentQuery.findCommentsWithAuthorByEventIdAndState(EVENT_ID_1,
                CommentState.PUBLISHED);

        assertThat(result).isEmpty();
        verify(commentRepository).findCommentsByEventIdAndStateWithAuthorFetch(EVENT_ID_1, CommentState.PUBLISHED);
        verifyNoMoreInteractions(commentRepository);
    }

    @Test
    void testFindCommentsWithAuthorByEventIdAndStateMapsFieldsCorrect() {
        Comment comment = createComment(COMMENT_ID_1, EVENT_ID_1, AUTHOR_ID_1);

        when(commentRepository.findCommentsByEventIdAndStateWithAuthorFetch(EVENT_ID_1, CommentState.PUBLISHED))
                .thenReturn(List.of(comment));
        List<CommentEventResponseDto> result = commentQuery.findCommentsWithAuthorByEventIdAndState(EVENT_ID_1,
                CommentState.PUBLISHED);

        assertThat(result).hasSize(1);

        CommentEventResponseDto dto = result.getFirst();
        assertThat(dto.getId()).isEqualTo(COMMENT_ID_1);
        assertThat(dto.getText()).isEqualTo(TEXT);
        assertThat(dto.getAuthor().getId()).isEqualTo(AUTHOR_ID_1);
        assertThat(dto.getAuthor().getName()).isEqualTo(AUTHOR_NAME);

        verify(commentRepository).findCommentsByEventIdAndStateWithAuthorFetch(EVENT_ID_1, CommentState.PUBLISHED);
        verifyNoMoreInteractions(commentRepository);
    }

    @Test
    void testFindCommentsWithAuthorByEventIdsAndStateGroupsByEventId() {
        List<Long> ids = List.of(EVENT_ID_1, EVENT_ID_2);

        Comment comment1 = createComment(COMMENT_ID_1, EVENT_ID_1, AUTHOR_ID_1);
        Comment comment2 = createComment(COMMENT_ID_2, EVENT_ID_1, AUTHOR_ID_2);
        Comment comment3 = createComment(COMMENT_ID_3, EVENT_ID_2, AUTHOR_ID_1);

        when(commentRepository.findCommentsByEventIdsAndStateWithAuthorFetch(ids, CommentState.PUBLISHED))
                .thenReturn(List.of(comment1, comment2, comment3));

        Map<Long, List<CommentEventResponseDto>> result = commentQuery.findCommentsWithAuthorByEventIdsAndState(ids,
                CommentState.PUBLISHED);

        assertThat(result).hasSize(2);
        assertThat(result.get(EVENT_ID_1))
                .hasSize(2)
                .extracting(CommentEventResponseDto::getId)
                .containsExactly(COMMENT_ID_1, COMMENT_ID_2);
        assertThat(result.get(EVENT_ID_2))
                .hasSize(1)
                .extracting(CommentEventResponseDto::getId)
                .containsExactly(COMMENT_ID_3);
        verify(commentRepository).findCommentsByEventIdsAndStateWithAuthorFetch(ids, CommentState.PUBLISHED);
        verifyNoMoreInteractions(commentRepository);
    }

    @Test
    void testFindCommentsWithAuthorByEventIdsAndStateReturnEmptyMap() {
        List<Long> ids = List.of(EVENT_ID_1);

        when(commentRepository.findCommentsByEventIdsAndStateWithAuthorFetch(ids, CommentState.PUBLISHED))
                .thenReturn(List.of());

        Map<Long, List<CommentEventResponseDto>> result = commentQuery.findCommentsWithAuthorByEventIdsAndState(ids,
                CommentState.PUBLISHED);

        assertThat(result).isEmpty();
        verify(commentRepository).findCommentsByEventIdsAndStateWithAuthorFetch(ids, CommentState.PUBLISHED);
        verifyNoMoreInteractions(commentRepository);
    }

    @Test
    void testFindCommentsWithAuthorByEventIdsAndStateNoKeyForEventWithoutComments() {
        List<Long> ids = List.of(EVENT_ID_1, EVENT_ID_2);

        Comment comment1 = createComment(COMMENT_ID_1, EVENT_ID_1, AUTHOR_ID_1);

        when(commentRepository.findCommentsByEventIdsAndStateWithAuthorFetch(ids, CommentState.PUBLISHED))
                .thenReturn(List.of(comment1));

        Map<Long, List<CommentEventResponseDto>> result = commentQuery.findCommentsWithAuthorByEventIdsAndState(ids,
                CommentState.PUBLISHED);

        assertThat(result).hasSize(1);
        assertThat(result).containsOnlyKeys(EVENT_ID_1);
        assertThat(result.get(EVENT_ID_1)).hasSize(1);
        verify(commentRepository).findCommentsByEventIdsAndStateWithAuthorFetch(ids, CommentState.PUBLISHED);
        verifyNoMoreInteractions(commentRepository);
    }

    private Comment createComment(Long id, Long eventId, Long authorId) {
        User author = new User();
        author.setId(authorId);
        author.setName(AUTHOR_NAME);
        author.setEmail("author-" + author + "@mail.com");

        Event event = Event.builder()
                .id(eventId)
                .build();

        return Comment.builder()
                .id(id)
                .event(event)
                .author(author)
                .text(TEXT)
                .state(CommentState.PUBLISHED)
                .build();
    }
}
