package ru.practicum.ewm.service.comment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.service.comment.dto.request.CommentAuthorRequestDto;
import ru.practicum.ewm.service.comment.dto.response.CommentAuthorResponseDto;
import ru.practicum.ewm.service.comment.model.CommentState;
import ru.practicum.ewm.service.comment.service.CommentService;
import ru.practicum.ewm.service.comment.utility.CommentPaginationParams;
import ru.practicum.ewm.service.exception.NotFoundException;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PrivateCommentController.class)
public class CommentPrivateControllerTest {
    private static final String API_PREFIX = "/users/{userId}/comments";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CommentService commentService;

    @Test
    void findAllReturnsCommentsWithDefaultPagination() throws Exception {
        LocalDateTime time = LocalDateTime.of(2026, 5, 15, 10, 0);
        CommentAuthorResponseDto response = createCommentAuthorResponseDto(time, "comment");
        CommentPaginationParams params = new CommentPaginationParams(0, 10, null);

        when(commentService.findAllByAuthor(2L, params))
                .thenReturn(List.of(response));

        mockMvc.perform(get(API_PREFIX, 2L))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.length()").value(1),
                        jsonPath("$[0].id").value(1),
                        jsonPath("$[0].eventId").value(3),
                        jsonPath("$[0].text").value("comment"),
                        jsonPath("$[0].state").value(CommentState.PENDING.name())
                );

        verify(commentService).findAllByAuthor(2L, params);
    }

    @Test
    void findAllWithNegativeFromReturnsBadRequestWithoutCallingService() throws Exception {
        mockMvc.perform(get(API_PREFIX, 2L)
                        .param("from", "-1"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(commentService);
    }

    @Test
    void findByIdReturnsCommentAndPassesIdsToService() throws Exception {
        LocalDateTime time = LocalDateTime.of(2026, 5, 15, 10, 0);
        CommentAuthorResponseDto response = createCommentAuthorResponseDto(time, "comment");

        when(commentService.findById(2L, 1L)).thenReturn(response);

        mockMvc.perform(get(API_PREFIX + "/{commentId}", 2L, 1L))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.id").value(1),
                        jsonPath("$.eventId").value(3),
                        jsonPath("$.text").value("comment"),
                        jsonPath("$.createdOn").value("2026-05-15 10:00:00"),
                        jsonPath("$.publishedOn").value(nullValue()),
                        jsonPath("$.state").value(CommentState.PENDING.name())
                );

        verify(commentService).findById(2L, 1L);
    }

    @Test
    void findByIdReturnsNotFoundWhenServiceThrowsNotFound() throws Exception {
        String message = "Комментарий id = 11 пользователя id = 22 не найден";

        when(commentService.findById(22L, 11L))
                .thenThrow(new NotFoundException("Комментарий id = 11 пользователя id = 22 не найден"));

        mockMvc.perform(get(API_PREFIX + "/{commentId}", 22L, 11L))
                .andExpectAll(
                        status().isNotFound(),
                        jsonPath("$.message").value(message)
                );

        verify(commentService).findById(22L, 11L);
    }

    @Test
    void createWithBlankTextReturnsBadRequestWithoutCallingService() throws Exception {
        CommentAuthorRequestDto request = new CommentAuthorRequestDto();
        request.setText("   ");

        mockMvc.perform(post(API_PREFIX, 1L)
                .param("eventId", "3")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        verifyNoInteractions(commentService);
    }

    @Test
    void createReturnsCreatedAndPassesRequestToService() throws Exception {
        LocalDateTime time = LocalDateTime.of(2026, 5, 15, 10, 0);
        CommentAuthorRequestDto request = new CommentAuthorRequestDto();
        request.setText("comment");

        CommentAuthorResponseDto response = createCommentAuthorResponseDto(time, "comment");
        when(commentService.create(2L, request, 3L))
                .thenReturn(response);

        mockMvc.perform(post(API_PREFIX, 2L)
                .param("eventId", "3")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpectAll(
                        status().isCreated(),
                        jsonPath("$.id").value(1),
                        jsonPath("$.eventId").value(3),
                        jsonPath("$.text").value("comment"),
                        jsonPath("$.createdOn").value("2026-05-15 10:00:00"),
                        jsonPath("$.publishedOn").value(nullValue()),
                        jsonPath("$.state").value(CommentState.PENDING.name())
                );

        verify(commentService).create(2L, request, 3L);
    }

    @Test
    void updateWithBlankTextReturnsBadRequestWithoutCallingService() throws Exception {
        CommentAuthorRequestDto request = new CommentAuthorRequestDto();
        request.setText("");

        mockMvc.perform(patch(API_PREFIX + "/{commentId}", 1L, 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(commentService);
    }

    @Test
    void updateCommentReturnsUpdatedComment() throws Exception {
        LocalDateTime time = LocalDateTime.of(2026, 5, 15, 10, 0);
        CommentAuthorRequestDto request = new CommentAuthorRequestDto();
        request.setText("new text");

        CommentAuthorResponseDto response = createCommentAuthorResponseDto(time, "new text");
        when(commentService.update(2L, request, 1L))
                .thenReturn(response);

        mockMvc.perform(patch(API_PREFIX + "/{commentId}", 2L, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"new text\"}"))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.id").value(1),
                        jsonPath("$.eventId").value(3),
                        jsonPath("$.text").value("new text"),
                        jsonPath("$.state").value(CommentState.PENDING.name())
                );

        verify(commentService).update(2L, request, 1L);
    }

    @Test
    void deleteCommentReturnsNotFound() throws Exception {
        String message = "Комментарий id = 11 пользователя id = 22 не найден";
        doThrow(new NotFoundException(message))
                .when(commentService).delete(22L, 11L);

        mockMvc.perform(delete(API_PREFIX + "/{commentId}", 22L, 11L))
                .andExpectAll(
                        status().isNotFound(),
                        jsonPath("$.message").value(message)
                );

        verify(commentService).delete(22L, 11L);
    }

    private CommentAuthorResponseDto createCommentAuthorResponseDto(LocalDateTime time, String text) {
        return CommentAuthorResponseDto.builder()
                .id(1L)
                .eventId(3L)
                .text(text)
                .createdOn(time)
                .state(CommentState.PENDING)
                .build();
    }
}
