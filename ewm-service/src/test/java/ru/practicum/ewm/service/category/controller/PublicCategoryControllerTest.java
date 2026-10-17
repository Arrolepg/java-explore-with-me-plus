package ru.practicum.ewm.service.category.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.service.category.dto.response.CategoryResponseDto;
import ru.practicum.ewm.service.category.service.CategoryService;
import ru.practicum.ewm.service.category.utility.CategoryPaginationParams;
import ru.practicum.ewm.service.exception.NotFoundException;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PublicCategoryController.class)
public class PublicCategoryControllerTest {
    private static final String API_PREFIX = "/categories";
    private static final Long CATEGORY_ID = 1L;
    private static final String NAME = "Концерты";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CategoryService categoryService;

    @Test
    void testGetById() throws Exception {
        when(categoryService.getById(CATEGORY_ID)).thenReturn(createResponseDto(CATEGORY_ID, NAME));

        mockMvc.perform(get(API_PREFIX + "/{catId}", CATEGORY_ID))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.id").value(CATEGORY_ID),
                        jsonPath("$.name").value(NAME)
                );

        verify(categoryService).getById(CATEGORY_ID);
    }

    @Test
    void testGetByIdNotFound() throws Exception {
        when(categoryService.getById(CATEGORY_ID))
                .thenThrow(new NotFoundException("Категория с id = 1 не найдена"));

        mockMvc.perform(get(API_PREFIX + "/{catId}", CATEGORY_ID))
                .andExpectAll(
                        status().isNotFound(),
                        jsonPath("$.status").value("NOT_FOUND"),
                        jsonPath("$.message").value("Категория с id = 1 не найдена")
                );
    }

    @Test
    void testGetByIdWithWrongIdReturnsBadRequest() throws Exception {
        mockMvc.perform(get(API_PREFIX + "/{catId}", "abc"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(categoryService);
    }

    @Test
    void testGetAllWithParams() throws Exception {
        when(categoryService.getAll(any(CategoryPaginationParams.class)))
                .thenReturn(List.of(createResponseDto(CATEGORY_ID, NAME), createResponseDto(2L, "Выставки")));

        mockMvc.perform(get(API_PREFIX).param("from", "5").param("size", "5"))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.length()").value(2),
                        jsonPath("$[0].id").value(CATEGORY_ID),
                        jsonPath("$[0].name").value(NAME),
                        jsonPath("$[1].id").value(2L),
                        jsonPath("$[1].name").value("Выставки")
                );

        verify(categoryService).getAll(new CategoryPaginationParams(5, 5));
    }

    @Test
    void testGetAllWithoutParamsUsesDefaults() throws Exception {
        when(categoryService.getAll(any(CategoryPaginationParams.class))).thenReturn(List.of());

        mockMvc.perform(get(API_PREFIX))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.length()").value(0)
                );

        verify(categoryService).getAll(new CategoryPaginationParams(0, 10));
    }

    @Test
    void testGetAllWithNegativeFromReturnsBadRequest() throws Exception {
        mockMvc.perform(get(API_PREFIX).param("from", "-1"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(categoryService);
    }

    @Test
    void testGetAllWithZeroSizeReturnsBadRequest() throws Exception {
        mockMvc.perform(get(API_PREFIX).param("size", "0"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(categoryService);
    }

    @Test
    void testGetAllWithWrongFromReturnsBadRequest() throws Exception {
        mockMvc.perform(get(API_PREFIX).param("from", "ad"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(categoryService);
    }

    private CategoryResponseDto createResponseDto(Long id, String name) {
        return CategoryResponseDto.builder()
                .id(id)
                .name(name)
                .build();
    }
}
