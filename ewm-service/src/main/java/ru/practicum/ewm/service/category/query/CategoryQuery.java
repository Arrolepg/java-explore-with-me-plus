package ru.practicum.ewm.service.category.query;

import ru.practicum.ewm.service.category.model.Category;

public interface CategoryQuery {
    Category findCategory(Long id);
}
