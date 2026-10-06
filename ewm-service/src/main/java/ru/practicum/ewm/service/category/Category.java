package ru.practicum.ewm.service.category;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Модель данных категорий
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "category")
public class Category {
    /**
     * Идентификатор категории
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Название категории
     */
    private String name;
}
