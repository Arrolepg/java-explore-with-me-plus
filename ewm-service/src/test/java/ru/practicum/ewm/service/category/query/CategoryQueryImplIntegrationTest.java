package ru.practicum.ewm.service.category.query;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.service.category.model.Category;
import ru.practicum.ewm.service.category.repository.CategoryRepository;
import ru.practicum.ewm.service.exception.NotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class CategoryQueryImplIntegrationTest {
    private static final String NAME = "Concerts";

    @Autowired
    private CategoryQuery categoryQuery;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void testFindCategory() {
        Category category = createCategory(NAME);

        Category result = categoryQuery.findCategory(category.getId());

        assertThat(result.getId()).isEqualTo(category.getId());
        assertThat(result.getName()).isEqualTo(NAME);
    }

    @Test
    void testFindCategoryNotFound() {
        assertThatThrownBy(() -> categoryQuery.findCategory(999L))
                .isInstanceOf(NotFoundException.class);
    }

    private Category createCategory(String name) {
        Category category = new Category();
        category.setName(name);
        return categoryRepository.save(category);
    }
}