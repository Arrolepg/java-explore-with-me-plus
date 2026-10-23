package ru.practicum.ewm.service.comment.query;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.service.comment.dto.response.CommentEventResponseDto;
import ru.practicum.ewm.service.comment.model.CommentState;
import ru.practicum.ewm.service.comment.repository.CommentRepository;
import ru.practicum.ewm.service.comment.repository.projection.EventCommentsCount;
import ru.practicum.ewm.service.comment.utility.CommentMapper;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CommentQueryImpl implements CommentQuery {
    private final CommentRepository commentRepository;

    @Override
    public Map<Long, Long> countCommentsByEventIdsAndState(List<Long> eventIds, CommentState commentState) {
        return commentRepository.countCommentsByEventIdsAndState(eventIds, commentState).stream()
                .collect(Collectors.toMap(
                        EventCommentsCount::eventId,
                        EventCommentsCount::commentsCount
                ));
    }

    @Override
    public List<CommentEventResponseDto> findCommentsWithAuthorByEventIdAndState(Long eventId, CommentState commentState) {
        return CommentMapper.toListCommentEventResponseDto(
                commentRepository.findCommentsByEventIdAndStateWithAuthorFetch(eventId, commentState)
        );
    }

    @Override
    public Map<Long, List<CommentEventResponseDto>> findCommentsWithAuthorByEventIdsAndState(List<Long> eventIds, CommentState commentState) {
        return commentRepository.findCommentsByEventIdsAndStateWithAuthorFetch(eventIds, commentState).stream()
                .collect(Collectors.groupingBy(
                        comment -> comment.getEvent().getId(),
                        Collectors.mapping(CommentMapper::toCommentEventResponseDto, Collectors.toList())
                ));
    }

    @Override
    public long countCommentsByEventIdAndState(Long eventId, CommentState commentState) {
        return commentRepository.countCommentsByEventIdAndState(eventId, commentState);
    }


}
