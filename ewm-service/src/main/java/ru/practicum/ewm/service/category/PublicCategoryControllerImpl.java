package ru.practicum.ewm.service.category;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@RestController
@Slf4j
@RequestMapping("/categories")
public class PublicCategoryControllerImpl implements PublicCategoryController {
    private final CategoryService categoryService;

    @Autowired
    public PublicCategoryControllerImpl(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/{id}")
    @Override
    public CategoryDto get(@Valid @PathVariable Long id) {
        return categoryService.get(id);
    }

    @GetMapping
    @Override
    public Collection<CategoryDto> getAll() {
        return categoryService.getAll();
    }

}
