package ru.practicum.ewm.service.comment.utility;


import lombok.experimental.UtilityClass;
import ru.practicum.ewm.service.comment.dto.response.CommentAuthorResponseDto;
import ru.practicum.ewm.service.comment.dto.response.CommentEventResponseDto;
import ru.practicum.ewm.service.comment.dto.response.CommentResponseDto;
import ru.practicum.ewm.service.comment.model.Comment;
import ru.practicum.ewm.service.event.dto.response.EventShortDto;
import ru.practicum.ewm.service.user.utility.UserMapper;

import java.util.List;
import java.util.Map;

@UtilityClass
public class CommentMapper {
    public CommentAuthorResponseDto toCommentAuthorResponseDto(Comment comment) {
        return CommentAuthorResponseDto.builder()
                .id(comment.getId())
                .eventId(comment.getEvent().getId())
                .text(comment.getText())
                .createdOn(comment.getCreatedOn())
                .publishedOn(comment.getPublishedOn())
                .state(comment.getState())
                .build();
    }

    public List<CommentAuthorResponseDto> toListCommentAuthorResponseDto(List<Comment> comments) {
        return comments.stream()
                .map(CommentMapper::toCommentAuthorResponseDto)
                .toList();
    }

    public CommentEventResponseDto toCommentEventResponseDto(Comment comment) {
        return CommentEventResponseDto.builder()
                .id(comment.getId())
                .author(UserMapper.toUserShortDto(comment.getAuthor()))
                .text(comment.getText())
                .createdOn(comment.getCreatedOn())
                .publishedOn(comment.getPublishedOn())
                .state(comment.getState())
                .build();
    }

    public List<CommentEventResponseDto> toListCommentEventResponseDto(List<Comment> comments) {
        return comments.stream()
                .map(CommentMapper::toCommentEventResponseDto)
                .toList();
    }

    public static CommentResponseDto toCommentResponseDto(Comment comment) {
        return CommentResponseDto.builder()
                .id(comment.getId())
                .author(UserMapper.toUserShortDto(comment.getAuthor()))
                .text(comment.getText())
                .createdOn(comment.getCreatedOn())
                .publishedOn(comment.getPublishedOn())
                .state(comment.getState())
                .event(null)
                .build();
    }

    public static CommentResponseDto toCommentResponseDto(Comment comment, EventShortDto eventShortDto) {
        CommentResponseDto dto = toCommentResponseDto(comment);
        dto.setEvent(eventShortDto);
        return dto;
    }

    public static List<CommentResponseDto> toListCommentResponseDto(List<Comment> comments, Map<Long, EventShortDto> eventsMap) {
        return comments.stream()
                .map(comment -> {
                    CommentResponseDto dto = CommentMapper.toCommentResponseDto(comment);
                    Long eventId = comment.getEvent().getId();
                    if (eventsMap != null && eventsMap.containsKey(eventId)) {
                        dto.setEvent(eventsMap.get(eventId));
                    }
                    return dto;
                })
                .toList();
    }
}
