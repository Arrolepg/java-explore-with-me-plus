package ru.practicum.ewm.service.category.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.service.category.dto.request.CategoryCreateDto;
import ru.practicum.ewm.service.category.dto.request.CategoryUpdateDto;
import ru.practicum.ewm.service.category.dto.response.CategoryResponseDto;
import ru.practicum.ewm.service.category.service.CategoryService;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/admin/categories")
public class AdminCategoryController {
    private final CategoryService categoryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponseDto create(@Valid @RequestBody CategoryCreateDto createDto) {
        log.info("Получен запрос на создание категории category = {}", createDto);

        return categoryService.create(createDto);
    }

    @PatchMapping("/{catId}")
    public CategoryResponseDto update(@PathVariable Long catId,
                                      @Valid @RequestBody CategoryUpdateDto updateDto) {
        log.info("Получен запрос на обновление категории с id = {}", catId);

        return categoryService.update(catId, updateDto);
    }

    @DeleteMapping("/{catId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long catId) {
        categoryService.delete(catId);
    }

}
