package ru.practicum.ewm.service.category.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import ru.practicum.ewm.service.category.dto.request.CategoryCreateDto;
import ru.practicum.ewm.service.category.dto.request.CategoryUpdateDto;
import ru.practicum.ewm.service.category.dto.response.CategoryResponseDto;
import ru.practicum.ewm.service.category.model.Category;
import ru.practicum.ewm.service.category.query.CategoryQuery;
import ru.practicum.ewm.service.category.repository.CategoryRepository;
import ru.practicum.ewm.service.category.utility.CategoryPaginationParams;
import ru.practicum.ewm.service.event.query.EventQuery;
import ru.practicum.ewm.service.exception.ConflictException;
import ru.practicum.ewm.service.exception.NotFoundException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CategoryServiceImplUnitTest {
    private static final Long CATEGORY_ID = 1L;
    private static final String NAME = "Concerts";
    private static final String NEW_NAME = "Exhibitions";

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryQuery categoryQuery;

    @Mock
    private EventQuery eventQuery;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    @Test
    void testGetById() {
        when(categoryQuery.findCategory(CATEGORY_ID)).thenReturn(createCategory(CATEGORY_ID, NAME));

        CategoryResponseDto result = categoryService.getById(CATEGORY_ID);

        assertAll(
                () -> assertThat(result.getId()).isEqualTo(CATEGORY_ID),
                () -> assertThat(result.getName()).isEqualTo(NAME)
        );
        verify(categoryQuery).findCategory(CATEGORY_ID);
        verifyNoInteractions(categoryRepository, eventQuery);
    }

    @Test
    void testGetByIdNotFound() {
        when(categoryQuery.findCategory(CATEGORY_ID))
                .thenThrow(new NotFoundException("Категория с id = 1 не найдена"));

        assertThatThrownBy(() -> categoryService.getById(CATEGORY_ID))
                .isInstanceOf(NotFoundException.class);
        verifyNoInteractions(categoryRepository, eventQuery);
    }

    @Test
    void testCreate() {
        when(categoryRepository.existsCategoryByName(NAME)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category category = invocation.getArgument(0);
            category.setId(CATEGORY_ID);
            return category;
        });

        CategoryResponseDto result = categoryService.create(createCreateDto(NAME));

        assertAll(
                () -> assertThat(result.getId()).isEqualTo(CATEGORY_ID),
                () -> assertThat(result.getName()).isEqualTo(NAME)
        );
        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo(NAME);
    }

    @Test
    void testCreateWithDuplicateNameThrowsConflict() {
        when(categoryRepository.existsCategoryByName(NAME)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.create(createCreateDto(NAME)))
                .isInstanceOf(ConflictException.class);
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void testUpdate() {
        Category category = createCategory(CATEGORY_ID, NAME);
        when(categoryQuery.findCategory(CATEGORY_ID)).thenReturn(category);
        when(categoryRepository.existsCategoryByNameAndIdNot(NEW_NAME, CATEGORY_ID)).thenReturn(false);
        when(categoryRepository.save(category)).thenReturn(category);

        CategoryResponseDto result = categoryService.update(CATEGORY_ID, createUpdateDto(NEW_NAME));

        assertAll(
                () -> assertThat(result.getId()).isEqualTo(CATEGORY_ID),
                () -> assertThat(result.getName()).isEqualTo(NEW_NAME)
        );
        verify(categoryRepository).save(category);
    }

    @Test
    void testUpdateWithNullNameKeepsCurrentName() {
        Category category = createCategory(CATEGORY_ID, NAME);
        when(categoryQuery.findCategory(CATEGORY_ID)).thenReturn(category);
        when(categoryRepository.save(category)).thenReturn(category);

        CategoryResponseDto result = categoryService.update(CATEGORY_ID, createUpdateDto(null));

        assertThat(result.getName()).isEqualTo(NAME);
        verify(categoryRepository, never()).existsCategoryByNameAndIdNot(anyString(), anyLong());
    }

    @Test
    void testUpdateWithDuplicateNameThrowsConflict() {
        when(categoryQuery.findCategory(CATEGORY_ID)).thenReturn(createCategory(CATEGORY_ID, NAME));
        when(categoryRepository.existsCategoryByNameAndIdNot(NEW_NAME, CATEGORY_ID)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.update(CATEGORY_ID, createUpdateDto(NEW_NAME)))
                .isInstanceOf(ConflictException.class);
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void testUpdateNotFound() {
        when(categoryQuery.findCategory(CATEGORY_ID))
                .thenThrow(new NotFoundException("Категория с id = 1 не найдена"));

        assertThatThrownBy(() -> categoryService.update(CATEGORY_ID, createUpdateDto(NEW_NAME)))
                .isInstanceOf(NotFoundException.class);
        verifyNoInteractions(categoryRepository);
    }

    @Test
    void testDelete() {
        when(categoryQuery.findCategory(CATEGORY_ID)).thenReturn(createCategory(CATEGORY_ID, NAME));
        when(eventQuery.existsByCategoryId(CATEGORY_ID)).thenReturn(false);

        categoryService.delete(CATEGORY_ID);

        verify(categoryRepository).deleteById(CATEGORY_ID);
    }

    @Test
    void testDeleteNotFound() {
        when(categoryQuery.findCategory(CATEGORY_ID))
                .thenThrow(new NotFoundException("Категория с id = 1 не найдена"));

        assertThatThrownBy(() -> categoryService.delete(CATEGORY_ID))
                .isInstanceOf(NotFoundException.class);
        verifyNoInteractions(eventQuery);
        verify(categoryRepository, never()).deleteById(any());
    }

    @Test
    void testDeleteWithEventsThrowsConflict() {
        when(categoryQuery.findCategory(CATEGORY_ID)).thenReturn(createCategory(CATEGORY_ID, NAME));
        when(eventQuery.existsByCategoryId(CATEGORY_ID)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.delete(CATEGORY_ID))
                .isInstanceOf(ConflictException.class);
        verify(categoryRepository, never()).deleteById(any());
    }

    @Test
    void testGetAll() {
        when(categoryRepository.findAllList(any(Pageable.class)))
                .thenReturn(List.of(createCategory(CATEGORY_ID, NAME), createCategory(2L, NEW_NAME)));

        List<CategoryResponseDto> result = categoryService.getAll(new CategoryPaginationParams(0, 10));

        assertThat(result).extracting(CategoryResponseDto::getId).containsExactly(CATEGORY_ID, 2L);
        assertThat(result).extracting(CategoryResponseDto::getName).containsExactly(NAME, NEW_NAME);
    }

    @Test
    void testGetAllBuildsPageableFromParams() {
        when(categoryRepository.findAllList(any(Pageable.class))).thenReturn(List.of());

        categoryService.getAll(new CategoryPaginationParams(10, 5));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(categoryRepository).findAllList(captor.capture());
        Pageable pageable = captor.getValue();
        assertAll(
                () -> assertThat(pageable.getPageNumber()).isEqualTo(2),
                () -> assertThat(pageable.getPageSize()).isEqualTo(5),
                () -> assertThat(pageable.getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "id"))
        );
    }

    @Test
    void testGetAllEmpty() {
        when(categoryRepository.findAllList(any(Pageable.class))).thenReturn(List.of());

        List<CategoryResponseDto> result = categoryService.getAll(new CategoryPaginationParams(null, null));

        assertThat(result).isEmpty();
    }

    private Category createCategory(Long id, String name) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        return category;
    }

    private CategoryCreateDto createCreateDto(String name) {
        CategoryCreateDto createDto = new CategoryCreateDto();
        createDto.setName(name);
        return createDto;
    }

    private CategoryUpdateDto createUpdateDto(String name) {
        CategoryUpdateDto updateDto = new CategoryUpdateDto();
        updateDto.setName(name);
        return updateDto;
    }
}
