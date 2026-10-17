package ru.practicum.ewm.service.category.utility;

import lombok.experimental.UtilityClass;
import ru.practicum.ewm.service.category.dto.request.CategoryCreateDto;
import ru.practicum.ewm.service.category.dto.response.CategoryResponseDto;
import ru.practicum.ewm.service.category.model.Category;

import java.util.List;

@UtilityClass
public class CategoryMapper {

    public static Category toCategory(CategoryCreateDto createDto) {
        return Category.builder()
                .name(createDto.getName())
                .build();
    }

    public CategoryResponseDto toCategoryResponseDto(Category category) {
        return CategoryResponseDto.builder()
                .id(category.getId())
                .name(category.getName())
                .build();
    }

    public List<CategoryResponseDto> toListCategoryResponseDto(List<Category> categories) {
        return categories.stream()
                .map(CategoryMapper::toCategoryResponseDto)
                .toList();
    }
}
