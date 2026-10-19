package ru.practicum.ewm.service.category.service;

import ru.practicum.ewm.service.category.dto.request.CategoryCreateDto;
import ru.practicum.ewm.service.category.dto.request.CategoryUpdateDto;
import ru.practicum.ewm.service.category.dto.response.CategoryResponseDto;
import ru.practicum.ewm.service.category.utility.CategoryPaginationParams;

import java.util.List;

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
    CategoryResponseDto getById(Long id);

    /**
     * Метод добавления новой категории
     *
     * @param createDto данные новой категории
     * @return объект новой категории
     */
    CategoryResponseDto create(CategoryCreateDto createDto);

    /**
     * Метод обновления данных о категории
     *
     * @param updateDto данные новой категории
     * @param id                идентификатор категории
     * @return объект обновленной категории
     */
    CategoryResponseDto update(Long id, CategoryUpdateDto updateDto);

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
    List<CategoryResponseDto> getAll(CategoryPaginationParams paginationParams);
}
