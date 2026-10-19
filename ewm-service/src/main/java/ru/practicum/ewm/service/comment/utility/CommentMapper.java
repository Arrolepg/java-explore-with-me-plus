package ru.practicum.ewm.service.comment.utility;

import lombok.experimental.UtilityClass;
import ru.practicum.ewm.service.comment.dto.response.CommentAuthorResponseDto;
import ru.practicum.ewm.service.comment.dto.response.CommentEventResponseDto;
import ru.practicum.ewm.service.comment.dto.response.CommentResponseDto;
import ru.practicum.ewm.service.comment.model.Comment;
import ru.practicum.ewm.service.user.utility.UserMapper;

import java.util.List;

@UtilityClass
public class CommentMapper {
    public Comment toComment() {
        return Comment.builder()
                .build();
    }

    public CommentAuthorResponseDto toCommentAuthorResponseDto() {
        return CommentAuthorResponseDto.builder()
                .build();
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

    public CommentResponseDto toCommentResponseDto() {
        return CommentResponseDto.builder()
                .build();
    }
}
