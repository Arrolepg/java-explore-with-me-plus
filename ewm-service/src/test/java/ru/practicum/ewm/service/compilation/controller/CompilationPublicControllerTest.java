package ru.practicum.ewm.service.compilation.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.service.compilation.dto.CompilationDto;
import ru.practicum.ewm.service.compilation.service.CompilationService;
import ru.practicum.ewm.service.compilation.utility.CompilationPaginationParams;
import ru.practicum.ewm.service.event.dto.response.EventShortDto;
import ru.practicum.ewm.service.exception.NotFoundException;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CompilationPublicController.class)
class CompilationPublicControllerTest {
    private static final String API_PREFIX = "/compilations";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CompilationService compilationService;

    @Test
    void findAllWithoutParametersUsesDefaultsAndReturnsEmptyArray() throws Exception {
        CompilationPaginationParams params = getParams(0, 10);

        when(compilationService.findAll(null, params)).thenReturn(List.of());

        mockMvc.perform(get(API_PREFIX))
                .andExpectAll(
                        status().isOk(),
                        content().json("[]")
                );

        verify(compilationService).findAll(null, params);
    }

    @Test
    void findAllPassesFilterAndPaginationToServiceAndReturnsList() throws Exception {
        CompilationPaginationParams params = getParams(3, 5);

        when(compilationService.findAll(false, params)).thenReturn(List.of(createCompilationDto()));

        mockMvc.perform(get(API_PREFIX)
                        .param("pinned", "false")
                        .param("from", "3")
                        .param("size", "5"))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.length()").value(1),
                        jsonPath("$[0].id").value(1),
                        jsonPath("$[0].title").value("Спектакли"),
                        jsonPath("$[0].pinned").value(false),
                        jsonPath("$[0].events.length()").value(1),
                        jsonPath("$[0].events[0].id").value(11)
                );

        verify(compilationService).findAll(false, params);
    }

    @Test
    void findAllReturnsBadRequestWhenFromIsNegative() throws Exception {
        mockMvc.perform(get(API_PREFIX)
                        .param("pinned", "false")
                        .param("from", "-1")
                        .param("size", "10"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(compilationService);
    }

    @Test
    void findAllReturnsBadRequestWhenSizeIsNil() throws Exception {
        mockMvc.perform(get(API_PREFIX)
                        .param("pinned", "false")
                        .param("from", "0")
                        .param("size", "0"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(compilationService);
    }

    @Test
    void findByIdReturnsCompilationWithEvents() throws Exception {
        when(compilationService.findById(1L)).thenReturn(createCompilationDto());

        mockMvc.perform(get(API_PREFIX + "/{compId}", 1L))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.id").value(1),
                        jsonPath("$.title").value("Спектакли"),
                        jsonPath("$.pinned").value(false),
                        jsonPath("$.events.length()").value(1),
                        jsonPath("$.events[0].id").value(11)
                );

        verify(compilationService).findById(1L);
    }

    @Test
    void findByIdMissingCompilationReturnsNotFound() throws Exception {
        when(compilationService.findById(99L))
                .thenThrow(new NotFoundException("Compilation with id=99 was not found"));

        mockMvc.perform(get(API_PREFIX + "/{compId}", 99L))
                .andExpectAll(
                        status().isNotFound(),
                        jsonPath("$.message").value("Compilation with id=99 was not found")
                );

        verify(compilationService).findById(99L);
    }

    private CompilationDto createCompilationDto() {
        return CompilationDto.builder()
                .id(1L)
                .title("Спектакли")
                .pinned(false)
                .events(List.of(EventShortDto.builder()
                        .id(11L)
                        .title("Спектакль")
                        .build()))
                .build();
    }

    private CompilationPaginationParams getParams(Integer from, Integer size) {
        return CompilationPaginationParams.builder()
                .from(from)
                .size(size)
                .build();
    }
}
