package ru.practicum.ewm.service.category;

import lombok.Builder;
import lombok.Data;

/**
 * DTO-объект для категории
 */
@Data
@Builder
public class CategoryDto {
    /**
     * Идентификатор категории
     */
    private Long id;

    /**
     * Название категории
     */
    private String name;
}
