package ru.practicum.ewm.service.category;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping("/admin/categories")
public class AdminCategoryControllerImpl implements AdminCategoryController {
    private final CategoryService categoryService;

    @Autowired
    public AdminCategoryControllerImpl(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Override
    public CategoryDto create(@RequestBody CategoryDto newCategory) {
        return categoryService.create(newCategory);
    }

    @PatchMapping("/{id}")
    @Override
    public CategoryDto update(@Valid @PathVariable Long id,
                              @RequestBody CategoryDto category) {
        return categoryService.update(category, id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Override
    public void delete(@Valid @PathVariable Long id) {
        categoryService.delete(id);
    }

}
