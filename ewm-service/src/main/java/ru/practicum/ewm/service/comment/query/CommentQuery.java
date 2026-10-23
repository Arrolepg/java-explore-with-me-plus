package ru.practicum.ewm.service.comment.query;

import ru.practicum.ewm.service.comment.dto.response.CommentEventResponseDto;
import ru.practicum.ewm.service.comment.model.CommentState;

import java.util.List;
import java.util.Map;

public interface CommentQuery {
    Map<Long, Long> countCommentsByEventIdsAndState(List<Long> eventIds, CommentState commentState);

    List<CommentEventResponseDto> findCommentsWithAuthorByEventIdAndState(Long eventId, CommentState commentState);

    Map<Long, List<CommentEventResponseDto>> findCommentsWithAuthorByEventIdsAndState(List<Long> eventIds,
                                                                                      CommentState commentState);

    long countCommentsByEventIdAndState(Long eventId, CommentState commentState);
}
