package ru.practicum.ewm.service.category;

/**
 * Интерфейс для контроллера по работе с категориями
 */
public interface AdminCategoryController {
    /**
     * Эндпоинт на добавление категории
     *
     * @param newCategory новая категоря
     * @return объект созданного пользователя
     */
    CategoryDto create(CategoryDto newCategory);

    /**
     * Эндпоинт на обновление данных о категории
     *
     * @param id       идентификатор обновляемой категории
     * @param category новые данные о категории
     * @return объект обновленной категории
     */
    CategoryDto update(Long id, CategoryDto category);

    /**
     * Эндпоинт удаления категории
     *
     * @param id идентфикатор категории
     */
    void delete(Long id);

}
