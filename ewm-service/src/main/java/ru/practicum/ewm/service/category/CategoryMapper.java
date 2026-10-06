package ru.practicum.ewm.service.category;

public class CategoryMapper {
    public static Category categoryDtoToCategory(CategoryDto categoryDto) {
        Category category = new Category();
        if (categoryDto.getName() != null) {
            category.setName(categoryDto.getName());
        }

        return category;
    }

    public static CategoryDto categoryToCategoryDto(Category category) {
        return CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .build();
    }
}
