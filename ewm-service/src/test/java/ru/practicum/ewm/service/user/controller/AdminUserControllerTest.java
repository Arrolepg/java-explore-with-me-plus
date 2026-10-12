package ru.practicum.ewm.service.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.service.exception.ConflictException;
import ru.practicum.ewm.service.exception.NotFoundException;
import ru.practicum.ewm.service.user.dto.NewUserRequest;
import ru.practicum.ewm.service.user.dto.UserDto;
import ru.practicum.ewm.service.user.service.UserService;
import ru.practicum.ewm.service.user.utility.AdminUserSearchRequest;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminUserController.class)
public class AdminUserControllerTest {
    private static final String API_PREFIX = "/admin/users";
    private static final Long USER_ID = 1L;
    private static final String EMAIL = "ivan.petrov@practicummail.ru";
    private static final String NAME = "Иван Петров";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @Test
    void testFindAllWithIds() throws Exception {
        when(userService.findAll(any(AdminUserSearchRequest.class))).thenReturn(List.of(createUserDto()));

        mockMvc.perform(get(API_PREFIX).param("ids", "1", "2").param("from", "0").param("size", "5"))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.length()").value(1),
                        jsonPath("$[0].id").value(USER_ID),
                        jsonPath("$[0].email").value(EMAIL),
                        jsonPath("$[0].name").value(NAME)
                );

        verify(userService).findAll(new AdminUserSearchRequest(List.of(1L, 2L), 0, 5));
    }

    @Test
    void testFindAllWithoutParamsUsesDefaults() throws Exception {
        when(userService.findAll(any(AdminUserSearchRequest.class))).thenReturn(List.of());

        mockMvc.perform(get(API_PREFIX))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.length()").value(0)
                );

        verify(userService).findAll(new AdminUserSearchRequest(null, 0, 10));
    }

    @Test
    void testFindAllWithWrongFromReturnsBadRequest() throws Exception {
        mockMvc.perform(get(API_PREFIX).param("from", "ad"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    @Test
    void testCreate() throws Exception {
        when(userService.create(any(NewUserRequest.class))).thenReturn(createUserDto());

        mockMvc.perform(post(API_PREFIX)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NewUserRequest(EMAIL, NAME))))
                .andExpectAll(
                        status().isCreated(),
                        jsonPath("$.id").value(USER_ID),
                        jsonPath("$.email").value(EMAIL),
                        jsonPath("$.name").value(NAME)
                );
    }

    @Test
    void testCreateWithBlankNameReturnsBadRequest() throws Exception {
        mockMvc.perform(post(API_PREFIX)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NewUserRequest(EMAIL, " "))))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    @Test
    void testCreateWithInvalidEmailReturnsBadRequest() throws Exception {
        mockMvc.perform(post(API_PREFIX)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NewUserRequest("not-an-email", NAME))))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    @Test
    void testCreateWithDuplicateEmailReturnsConflict() throws Exception {
        when(userService.create(any(NewUserRequest.class)))
                .thenThrow(new ConflictException("Пользователь с таким email уже существует"));

        mockMvc.perform(post(API_PREFIX)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NewUserRequest(EMAIL, NAME))))
                .andExpect(status().isConflict());
    }

    @Test
    void testDelete() throws Exception {
        mockMvc.perform(delete(API_PREFIX + "/{userId}", USER_ID))
                .andExpect(status().isNoContent());

        verify(userService).delete(USER_ID);
    }

    @Test
    void testDeleteNotFound() throws Exception {
        doThrow(new NotFoundException("Пользователь с id = 1 не найден")).when(userService).delete(USER_ID);

        mockMvc.perform(delete(API_PREFIX + "/{userId}", USER_ID))
                .andExpect(status().isNotFound());
    }

    private UserDto createUserDto() {
        return UserDto.builder()
                .id(USER_ID)
                .email(EMAIL)
                .name(NAME)
                .build();
    }
}
