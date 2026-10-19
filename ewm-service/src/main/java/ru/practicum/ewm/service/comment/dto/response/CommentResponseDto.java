package ru.practicum.ewm.service.comment.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import ru.practicum.common.util.PatternDataTime;
import ru.practicum.ewm.service.comment.model.CommentState;
import ru.practicum.ewm.service.event.dto.response.EventShortDto;
import ru.practicum.ewm.service.user.dto.UserShortDto;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
public class CommentResponseDto {
    private Long id;

    private EventShortDto event;

    private UserShortDto author;

    private String text;

    @JsonFormat(pattern = PatternDataTime.PATTERN)
    private LocalDateTime createdOn;

    @JsonFormat(pattern = PatternDataTime.PATTERN)
    @Builder.Default
    private LocalDateTime publishedOn = null;

    private CommentState state;
}
