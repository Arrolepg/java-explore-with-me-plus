package ru.practicum.ewm.service.compilation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.service.compilation.dto.CompilationDto;
import ru.practicum.ewm.service.compilation.service.CompilationService;
import ru.practicum.ewm.service.compilation.utility.CompilationPaginationParams;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/compilations")
public class CompilationPublicController {
    private final CompilationService compilationService;

    @GetMapping
    public List<CompilationDto> findAll(@RequestParam(required = false) Boolean pinned,
                                        @Valid @ModelAttribute CompilationPaginationParams paginationParams) {
        return compilationService.findAll(pinned, paginationParams);
    }

    @GetMapping("/{compId}")
    public CompilationDto findById(@PathVariable Long compId) {
        return compilationService.findById(compId);
    }
}
