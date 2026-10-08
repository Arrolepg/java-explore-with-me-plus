package ru.practicum.ewm.service.category.query;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.service.category.model.Category;
import ru.practicum.ewm.service.category.repository.CategoryRepository;
import ru.practicum.ewm.service.exception.NotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CategoryQueryImplUnitTest {
    private static final Long CATEGORY_ID = 1L;
    private static final String NAME = "Concerts";

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryQueryImpl categoryQuery;

    @Test
    void testFindCategory() {
        Category category = createCategory(CATEGORY_ID, NAME);
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));

        Category result = categoryQuery.findCategory(CATEGORY_ID);

        assertThat(result).isEqualTo(category);
        assertThat(result.getId()).isEqualTo(CATEGORY_ID);
        assertThat(result.getName()).isEqualTo(NAME);

        verify(categoryRepository, times(1)).findById(CATEGORY_ID);
        verifyNoMoreInteractions(categoryRepository);
    }

    @Test
    void testFindCategoryNotFound() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryQuery.findCategory(99L))
                .isInstanceOf(NotFoundException.class);

        verify(categoryRepository, times(1)).findById(99L);
        verifyNoMoreInteractions(categoryRepository);
    }

    private Category createCategory(Long id, String name) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        return category;
    }
}