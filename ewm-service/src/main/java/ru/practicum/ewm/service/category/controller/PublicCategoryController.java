package ru.practicum.ewm.service.category.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.service.category.dto.response.CategoryResponseDto;
import ru.practicum.ewm.service.category.service.CategoryService;
import ru.practicum.ewm.service.category.utility.CategoryPaginationParams;

import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/categories")
public class PublicCategoryController {
    private final CategoryService categoryService;

    @GetMapping("/{catId}")
    public CategoryResponseDto getById(@PathVariable Long catId) {
        log.info("Получен запрос на поиск категории с id = {}", catId);

        return categoryService.getById(catId);
    }

    @GetMapping
    public List<CategoryResponseDto> getAll(@Valid @ModelAttribute CategoryPaginationParams params) {
        log.info("Получен запрос на поиск всех категорий с параметрами запроса from = {}, size = {}",
                params.from(), params.size());

        return categoryService.getAll(params);
    }

}
