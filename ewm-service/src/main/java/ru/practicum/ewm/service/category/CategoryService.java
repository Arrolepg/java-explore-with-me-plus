package ru.practicum.ewm.service.category;

import java.util.Collection;

/**
 * Интерфейс реализует логику CRUD-операций для сущности Категория
 */
public interface CategoryService {
    /**
     * Метод получения категории
     *
     * @param id идентификатор категории
     * @return объект категории
     */
    CategoryDto get(Long id);

    /**
     * Метод добавления новой категории
     *
     * @param categoryDto данные новой категории
     * @return объект новой категории
     */
    CategoryDto create(CategoryDto categoryDto);

    /**
     * Метод обновления данных о категории
     *
     * @param categoryDto данные новой категории
     * @param id          идентификатор категории
     * @return объект обновленной категории
     */
    CategoryDto update(CategoryDto categoryDto, Long id);

    /**
     * Метод удаления категории
     *
     * @param id идентификатор категории
     */
    void delete(Long id);

    /**
     * Метод получения списка всех категорий
     *
     * @return список всех категорий
     */
    Collection<CategoryDto> getAll();
}
