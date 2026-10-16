package ru.practicum.ewm.service.category.repository;

import org.springframework.data.domain.Pageable;
import ru.practicum.ewm.service.category.model.Category;

import java.util.List;

public interface CategorySearchRepository {
    List<Category> findAllList(Pageable pageable);
}
