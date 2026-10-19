package ru.practicum.ewm.service.comment.utility;

import lombok.experimental.UtilityClass;
import ru.practicum.ewm.service.comment.dto.response.CommentAuthorResponseDto;
import ru.practicum.ewm.service.comment.dto.response.CommentEventResponseDto;
import ru.practicum.ewm.service.comment.dto.response.CommentResponseDto;
import ru.practicum.ewm.service.comment.model.Comment;

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

    public CommentEventResponseDto toCommentEventResponseDto() {
        return CommentEventResponseDto.builder()
                .build();
    }

    public CommentResponseDto toCommentResponseDto() {
        return CommentResponseDto.builder()
                .build();
    }
}
