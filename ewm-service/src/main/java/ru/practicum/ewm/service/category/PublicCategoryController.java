package ru.practicum.ewm.service.category;

import java.util.Collection;

/**
 * Интерфейс для контроллера по работе с категориями
 */
public interface PublicCategoryController {


    /**
     * Эндпоинт получения конкретной категории
     *
     * @param id идентификатор категории
     * @return объект категории
     */
    CategoryDto get(Long id);

    /**
     * Эндпоинт получения списка всех категорий
     *
     * @return список всех категорий
     */
    Collection<CategoryDto> getAll();
}
