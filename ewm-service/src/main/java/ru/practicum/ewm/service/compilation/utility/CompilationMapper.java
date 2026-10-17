package ru.practicum.ewm.service.compilation.utility;

import lombok.experimental.UtilityClass;
import ru.practicum.ewm.service.compilation.dto.CompilationDto;
import ru.practicum.ewm.service.compilation.dto.NewCompilationDto;
import ru.practicum.ewm.service.compilation.model.Compilation;
import ru.practicum.ewm.service.event.dto.response.EventShortDto;
import ru.practicum.ewm.service.event.model.Event;

import java.util.List;

@UtilityClass
public class CompilationMapper {
    public Compilation toCompilation(NewCompilationDto dto, List<Event> events) {
        return Compilation.builder()
                .events(events)
                .pinned(Boolean.TRUE.equals(dto.getPinned()))
                .title(dto.getTitle())
                .build();
    }

    public CompilationDto toCompilationDto(Compilation compilation, List<EventShortDto> events) {
        return CompilationDto.builder()
                .events(events)
                .id(compilation.getId())
                .pinned(compilation.getPinned())
                .title(compilation.getTitle())
                .build();
    }

}
