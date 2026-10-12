package ru.practicum.ewm.service.category.utility;

import lombok.experimental.UtilityClass;
import ru.practicum.ewm.service.category.dto.request.CategoryCreateDto;
import ru.practicum.ewm.service.category.dto.response.CategoryResponseDto;
import ru.practicum.ewm.service.category.model.Category;

@UtilityClass
public class CategoryMapper {

    public static Category toCategory(CategoryCreateDto categoryDto) {
        Category category = new Category();
        if (categoryDto.getName() != null) {
            category.setName(categoryDto.getName());
        }

        return category;
    }

    public CategoryResponseDto toCategoryDto(Category category) {
        return CategoryResponseDto.builder()
                .id(category.getId())
                .name(category.getName())
                .build();
    }

}
