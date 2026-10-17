package ru.practicum.ewm.service.compilation.repository;

import ru.practicum.ewm.service.compilation.model.Compilation;
import ru.practicum.ewm.service.compilation.utility.CompilationPaginationParams;

import java.util.List;

public interface CompilationSearchRepository {
    List<Compilation> findCompilations(Boolean pinned, CompilationPaginationParams params);
}
