package ru.practicum.ewm.service.compilation.service;

import ru.practicum.ewm.service.compilation.dto.CompilationDto;
import ru.practicum.ewm.service.compilation.dto.NewCompilationDto;
import ru.practicum.ewm.service.compilation.dto.UpdateCompilationRequest;
import ru.practicum.ewm.service.compilation.utility.CompilationPaginationParams;

import java.util.List;

public interface CompilationService {
    CompilationDto create(NewCompilationDto dto);

    CompilationDto update(Long compId, UpdateCompilationRequest dto);

    void delete(Long compId);

    CompilationDto findById(Long compId);

    List<CompilationDto> findAll(Boolean pinned, CompilationPaginationParams params);
}
