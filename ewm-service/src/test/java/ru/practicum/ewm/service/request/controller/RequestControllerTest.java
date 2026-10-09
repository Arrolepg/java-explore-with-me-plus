package ru.practicum.ewm.service.request.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.service.exception.ConflictException;
import ru.practicum.ewm.service.exception.NotFoundException;
import ru.practicum.ewm.service.request.service.RequestService;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RequestController.class)
public class RequestControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    RequestService requestService;

    @Test
    void testFindAllByRequesterShouldBeEmpty() throws Exception {
        when(requestService.findAllByRequester(1L))
                .thenReturn(List.of());

        mockMvc.perform(get("/users/{userId}/requests", 1L))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void testCreateRequestThrowsConflictWhenNewRequestIsCopy() throws Exception {
        when(requestService.create(1L, 2L))
                .thenThrow(new ConflictException("Запрос был создан ранее"));

        mockMvc.perform(post("/users/{userId}/requests", 1L)
                .param("eventId", "2"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Запрос был создан ранее"));
    }

    @Test
    void test() throws Exception {
        when(requestService.cancelRequest(1L, 2L))
                .thenThrow(new NotFoundException("Запрос на участие id = " + 2L
                        + " для пользователя id = " + 1L + " не найден"));

        mockMvc.perform(patch("/users/{userId}/requests/{requestId}/cancel", 1L, 2L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Запрос на участие id = " + 2L
                        + " для пользователя id = " + 1L + " не найден"));
    }
}
