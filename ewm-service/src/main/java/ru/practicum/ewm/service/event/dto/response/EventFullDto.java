package ru.practicum.ewm.service.event.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import ru.practicum.common.util.PatternDataTime;
import ru.practicum.ewm.service.category.dto.response.CategoryResponseDto;
import ru.practicum.ewm.service.comment.dto.response.CommentEventResponseDto;
import ru.practicum.ewm.service.event.model.EventState;
import ru.practicum.ewm.service.user.dto.UserShortDto;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class EventFullDto {
    private Long id;

    private String annotation;

    private CategoryResponseDto category;

    @Builder.Default
    private Long confirmedRequests = 0L;

    @JsonFormat(pattern = PatternDataTime.PATTERN)
    private LocalDateTime createdOn;

    private String description;

    @JsonFormat(pattern = PatternDataTime.PATTERN)
    private LocalDateTime eventDate;

    private UserShortDto initiator;

    private EventLocationDto location;

    private Boolean paid;

    private Integer participantLimit;

    @JsonFormat(pattern = PatternDataTime.PATTERN)
    @Builder.Default
    private LocalDateTime publishedOn = null;

    private Boolean requestModeration;

    private EventState state;

    private String title;

    @Builder.Default
    private Long views = 0L;

    @Builder.Default
    List<CommentEventResponseDto> comments = List.of();
}
