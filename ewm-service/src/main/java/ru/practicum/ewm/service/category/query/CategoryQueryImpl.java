package ru.practicum.ewm.service.category.query;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.service.category.model.Category;
import ru.practicum.ewm.service.category.repository.CategoryRepository;
import ru.practicum.ewm.service.exception.NotFoundException;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryQueryImpl implements CategoryQuery {
    private final CategoryRepository categoryRepository;

    @Override
    public Category findCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(
                        () -> new NotFoundException("Катeгория с id = " + id + " не найдена")
                );
    }
}
