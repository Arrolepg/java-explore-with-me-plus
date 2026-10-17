package ru.practicum.ewm.service.category.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.ewm.service.category.model.Category;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@ActiveProfiles("test")
public class CategorySearchRepositoryTest {
    private static final Sort SORT_BY_ID_ASC = Sort.by(Sort.Direction.ASC, "id");

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category first;
    private Category second;
    private Category third;

    @BeforeEach
    void setUp() {
        first = createCategory("Concerts");
        second = createCategory("Exhibitions");
        third = createCategory("Art");
        entityManager.clear();
    }

    @Test
    void testFindAllListSortAscById() {
        List<Category> result = categoryRepository.findAllList(PageRequest.of(0, 10, SORT_BY_ID_ASC));

        assertThat(result).extracting(Category::getId)
                .containsExactly(first.getId(), second.getId(), third.getId());
    }

    @Test
    void testFindAllListSortDescByName() {
        List<Category> result = categoryRepository.findAllList(
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "name")));

        assertThat(result).extracting(Category::getName)
                .containsExactly("Exhibitions", "Concerts", "Art");
    }

    @Test
    void testFindAllListPagination() {
        List<Category> firstPage = categoryRepository.findAllList(PageRequest.of(0, 2, SORT_BY_ID_ASC));
        List<Category> secondPage = categoryRepository.findAllList(PageRequest.of(1, 2, SORT_BY_ID_ASC));

        assertThat(firstPage).extracting(Category::getId).containsExactly(first.getId(), second.getId());
        assertThat(secondPage).extracting(Category::getId).containsExactly(third.getId());
    }

    @Test
    void testFindAllListPageOutOfRange() {
        List<Category> result = categoryRepository.findAllList(PageRequest.of(5, 10, SORT_BY_ID_ASC));

        assertThat(result).isEmpty();
    }

    @Test
    void testFindAllListUnpaged() {
        List<Category> result = categoryRepository.findAllList(Pageable.unpaged());

        assertThat(result).extracting(Category::getId)
                .containsExactlyInAnyOrder(first.getId(), second.getId(), third.getId());
    }

    @Test
    void testExistsCategoryByName() {
        assertThat(categoryRepository.existsCategoryByName("Concerts")).isTrue();
        assertThat(categoryRepository.existsCategoryByName("Cinema")).isFalse();
    }

    @Test
    void testExistsCategoryByNameAndIdNot() {
        assertThat(categoryRepository.existsCategoryByNameAndIdNot("Concerts", first.getId())).isFalse();
        assertThat(categoryRepository.existsCategoryByNameAndIdNot("Concerts", second.getId())).isTrue();
    }

    private Category createCategory(String name) {
        Category category = new Category();
        category.setName(name);
        return entityManager.persistAndFlush(category);
    }
}
