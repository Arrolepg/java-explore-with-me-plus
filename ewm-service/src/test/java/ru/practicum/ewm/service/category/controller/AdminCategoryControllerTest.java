package ru.practicum.ewm.service.category.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.service.category.dto.request.CategoryCreateDto;
import ru.practicum.ewm.service.category.dto.request.CategoryUpdateDto;
import ru.practicum.ewm.service.category.dto.response.CategoryResponseDto;
import ru.practicum.ewm.service.category.service.CategoryService;
import ru.practicum.ewm.service.exception.ConflictException;
import ru.practicum.ewm.service.exception.NotFoundException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminCategoryController.class)
public class AdminCategoryControllerTest {
    private static final String API_PREFIX = "/admin/categories";
    private static final Long CATEGORY_ID = 1L;
    private static final String NAME = "Концерты";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CategoryService categoryService;

    @Test
    void testCreate() throws Exception {
        when(categoryService.create(any(CategoryCreateDto.class))).thenReturn(createResponseDto(NAME));

        mockMvc.perform(post(API_PREFIX)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCreateDto(NAME))))
                .andExpectAll(
                        status().isCreated(),
                        jsonPath("$.id").value(CATEGORY_ID),
                        jsonPath("$.name").value(NAME)
                );

        verify(categoryService).create(createCreateDto(NAME));
    }

    @Test
    void testCreateWithMaxLengthName() throws Exception {
        String name = "a".repeat(50);
        when(categoryService.create(any(CategoryCreateDto.class))).thenReturn(createResponseDto(name));

        mockMvc.perform(post(API_PREFIX)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCreateDto(name))))
                .andExpectAll(
                        status().isCreated(),
                        jsonPath("$.name").value(name)
                );
    }

    @Test
    void testCreateWithoutNameReturnsBadRequest() throws Exception {
        mockMvc.perform(post(API_PREFIX)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath("$.status").value("BAD_REQUEST")
                );

        verifyNoInteractions(categoryService);
    }

    @Test
    void testCreateWithBlankNameReturnsBadRequest() throws Exception {
        mockMvc.perform(post(API_PREFIX)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCreateDto("   "))))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(categoryService);
    }

    @Test
    void testCreateWithTooLongNameReturnsBadRequest() throws Exception {
        mockMvc.perform(post(API_PREFIX)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCreateDto("a".repeat(51)))))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(categoryService);
    }

    @Test
    void testCreateWithoutBodyReturnsBadRequest() throws Exception {
        mockMvc.perform(post(API_PREFIX)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(categoryService);
    }

    @Test
    void testCreateWithDuplicateNameReturnsConflict() throws Exception {
        when(categoryService.create(any(CategoryCreateDto.class)))
                .thenThrow(new ConflictException("Категория с названием " + NAME + " уже существует"));

        mockMvc.perform(post(API_PREFIX)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCreateDto(NAME))))
                .andExpectAll(
                        status().isConflict(),
                        jsonPath("$.status").value("CONFLICT")
                );
    }

    @Test
    void testUpdate() throws Exception {
        when(categoryService.update(eq(CATEGORY_ID), any(CategoryUpdateDto.class)))
                .thenReturn(createResponseDto("Выставки"));

        mockMvc.perform(patch(API_PREFIX + "/{catId}", CATEGORY_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createUpdateDto("Выставки"))))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.id").value(CATEGORY_ID),
                        jsonPath("$.name").value("Выставки")
                );

        verify(categoryService).update(CATEGORY_ID, createUpdateDto("Выставки"));
    }

    @Test
    void testUpdateWithTooLongNameReturnsBadRequest() throws Exception {
        mockMvc.perform(patch(API_PREFIX + "/{catId}", CATEGORY_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createUpdateDto("a".repeat(51)))))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(categoryService);
    }

    @Test
    void testUpdateWithWrongIdReturnsBadRequest() throws Exception {
        mockMvc.perform(patch(API_PREFIX + "/{catId}", "abc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createUpdateDto(NAME))))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(categoryService);
    }

    @Test
    void testUpdateNotFound() throws Exception {
        when(categoryService.update(eq(CATEGORY_ID), any(CategoryUpdateDto.class)))
                .thenThrow(new NotFoundException("Категория с id = 1 не найдена"));

        mockMvc.perform(patch(API_PREFIX + "/{catId}", CATEGORY_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createUpdateDto(NAME))))
                .andExpectAll(
                        status().isNotFound(),
                        jsonPath("$.status").value("NOT_FOUND")
                );
    }

    @Test
    void testUpdateWithDuplicateNameReturnsConflict() throws Exception {
        when(categoryService.update(eq(CATEGORY_ID), any(CategoryUpdateDto.class)))
                .thenThrow(new ConflictException("Категория с названием " + NAME + " уже существует"));

        mockMvc.perform(patch(API_PREFIX + "/{catId}", CATEGORY_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createUpdateDto(NAME))))
                .andExpect(status().isConflict());
    }

    @Test
    void testDelete() throws Exception {
        mockMvc.perform(delete(API_PREFIX + "/{catId}", CATEGORY_ID))
                .andExpect(status().isNoContent());

        verify(categoryService).delete(CATEGORY_ID);
    }

    @Test
    void testDeleteNotFound() throws Exception {
        doThrow(new NotFoundException("Категория с id = 1 не найдена")).when(categoryService).delete(CATEGORY_ID);

        mockMvc.perform(delete(API_PREFIX + "/{catId}", CATEGORY_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteWithEventsReturnsConflict() throws Exception {
        doThrow(new ConflictException("Категория не может быть удалена, так как с ней связано событие"))
                .when(categoryService).delete(CATEGORY_ID);

        mockMvc.perform(delete(API_PREFIX + "/{catId}", CATEGORY_ID))
                .andExpect(status().isConflict());
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

    private CategoryResponseDto createResponseDto(String name) {
        return CategoryResponseDto.builder()
                .id(CATEGORY_ID)
                .name(name)
                .build();
    }
}
