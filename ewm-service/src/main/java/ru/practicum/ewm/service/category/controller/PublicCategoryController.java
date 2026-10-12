package ru.practicum.ewm.service.category.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.service.category.dto.response.CategoryResponseDto;
import ru.practicum.ewm.service.category.service.CategoryService;
import ru.practicum.ewm.service.category.utility.PublicCategorySearchRequest;

import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/categories")
public class PublicCategoryController {
    private final CategoryService categoryService;

    @GetMapping("/{id}")
    public CategoryResponseDto get(@Valid @PathVariable Long id) {
        return categoryService.get(id);
    }

    @GetMapping
    public List<CategoryResponseDto> getAll(@RequestParam(required = false, defaultValue = "0") Integer from,
                                            @RequestParam(required = false, defaultValue = "10") Integer size) {
        PublicCategorySearchRequest publicCategorySearchRequest = new PublicCategorySearchRequest(from, size);
        return categoryService.getAll(publicCategorySearchRequest);
    }

}
