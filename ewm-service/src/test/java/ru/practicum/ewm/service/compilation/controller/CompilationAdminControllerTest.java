package ru.practicum.ewm.service.compilation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.service.compilation.dto.CompilationDto;
import ru.practicum.ewm.service.compilation.dto.NewCompilationDto;
import ru.practicum.ewm.service.compilation.dto.UpdateCompilationRequest;
import ru.practicum.ewm.service.compilation.service.CompilationService;
import ru.practicum.ewm.service.event.dto.response.EventShortDto;
import ru.practicum.ewm.service.exception.NotFoundException;

import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CompilationAdminController.class)
class CompilationAdminControllerTest {
    private static final String API_PREFIX = "/admin/compilations";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CompilationService compilationService;

    @Test
    void createReturnsCreatedAndPassesRequestToService() throws Exception {
        NewCompilationDto request = new NewCompilationDto();
        request.setTitle("Летние концерты");
        request.setPinned(true);
        request.setEvents(List.of(11L, 12L));

        CompilationDto response = CompilationDto.builder()
                .id(1L)
                .title("Летние концерты")
                .pinned(true)
                .events(List.of(
                        EventShortDto.builder().id(11L).title("Первый концерт").build(),
                        EventShortDto.builder().id(12L).title("Второй концерт").build()
                ))
                .build();
        when(compilationService.create(request)).thenReturn(response);

        mockMvc.perform(post(API_PREFIX)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpectAll(
                        status().isCreated(),
                        jsonPath("$.id").value(1),
                        jsonPath("$.title").value("Летние концерты"),
                        jsonPath("$.pinned").value(true),
                        jsonPath("$.events.length()").value(2),
                        jsonPath("$.events[0].id").value(11),
                        jsonPath("$.events[1].id").value(12)
                );

        verify(compilationService).create(request);
    }

    @Test
    void createWithBlankTitleReturnsBadRequestWithoutCallingService() throws Exception {
        NewCompilationDto request = new NewCompilationDto();
        request.setTitle(" ");

        mockMvc.perform(post(API_PREFIX)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(compilationService);
    }

    @Test
    void updateWithOnlyPinnedReturnsUpdatedCompilation() throws Exception {
        UpdateCompilationRequest request = new UpdateCompilationRequest();
        request.setPinned(false);

        CompilationDto response = CompilationDto.builder()
                .id(1L)
                .title("Летние концерты")
                .pinned(false)
                .events(List.of())
                .build();
        when(compilationService.update(1L, request)).thenReturn(response);

        mockMvc.perform(patch(API_PREFIX + "/{compId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pinned\":false}"))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.id").value(1),
                        jsonPath("$.title").value("Летние концерты"),
                        jsonPath("$.pinned").value(false),
                        jsonPath("$.events").isEmpty()
                );

        verify(compilationService).update(1L, request);
    }

    @Test
    void updateWithEmptyTitleReturnsBadRequestWithoutCallingService() throws Exception {
        UpdateCompilationRequest request = new UpdateCompilationRequest();
        request.setTitle("");

        mockMvc.perform(patch(API_PREFIX + "/{compId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(compilationService);
    }

    @Test
    void deleteReturnsNoContentAndPassesIdToService() throws Exception {
        mockMvc.perform(delete(API_PREFIX + "/{compId}", 1L))
                .andExpectAll(
                        status().isNoContent(),
                        content().string("")
                );

        verify(compilationService).delete(1L);
    }

    @Test
    void deleteMissingCompilationReturnsNotFound() throws Exception {
        doThrow(new NotFoundException("Compilation with id=99 was not found"))
                .when(compilationService).delete(99L);

        mockMvc.perform(delete(API_PREFIX + "/{compId}", 99L))
                .andExpectAll(
                        status().isNotFound(),
                        jsonPath("$.message").value("Compilation with id=99 was not found")
                );

        verify(compilationService).delete(99L);
    }
}
