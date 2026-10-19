package ru.practicum.ewm.service.compilation.dto;

import lombok.Builder;
import lombok.Data;
import ru.practicum.ewm.service.event.dto.response.EventShortDto;

import java.util.List;

@Data
@Builder
public class CompilationDto {
    private Long id;
    private Boolean pinned;
    private String title;
    private List<EventShortDto> events;
}
