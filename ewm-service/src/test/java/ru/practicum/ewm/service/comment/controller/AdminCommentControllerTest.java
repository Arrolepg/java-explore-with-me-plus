package ru.practicum.ewm.service.comment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.service.category.dto.response.CategoryResponseDto;
import ru.practicum.ewm.service.comment.dto.request.CommentAdminUpdateDto;
import ru.practicum.ewm.service.comment.dto.response.CommentResponseDto;
import ru.practicum.ewm.service.comment.model.AdminCommentStateAction;
import ru.practicum.ewm.service.comment.model.CommentState;
import ru.practicum.ewm.service.comment.service.CommentService;
import ru.practicum.ewm.service.comment.utility.AdminCommentSearchFilter;
import ru.practicum.ewm.service.comment.utility.CommentPaginationParams;
import ru.practicum.ewm.service.event.dto.response.EventShortDto;
import ru.practicum.ewm.service.exception.ConflictException;
import ru.practicum.ewm.service.exception.NotFoundException;
import ru.practicum.ewm.service.user.dto.UserShortDto;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminCommentController.class)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class AdminCommentControllerTest {
    private static final Long COMMENT_ID = 1L;
    private static final String API_PREFIX = "/admin/comments";
    private static final String TEXT = "Test comment text";

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    @MockBean
    private CommentService commentService;

    @Test
    void testFindAllWithAllParams() throws Exception {
        when(commentService.findAllByAdmin(any(AdminCommentSearchFilter.class), any(CommentPaginationParams.class)))
                .thenReturn(List.of(createCommentResponseDto(CommentState.PUBLISHED)));

        mockMvc.perform(get(API_PREFIX)
                        .param("users", "1", "2")
                        .param("events", "10", "11")
                        .param("state", "PUBLISHED")
                        .param("from", "5")
                        .param("size", "5"))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.length()").value(1),
                        jsonPath("$[0].id").value(COMMENT_ID),
                        jsonPath("$[0].state").value("PUBLISHED")
                );

        verify(commentService).findAllByAdmin(argThat(request -> {
                    assertAll(
                            () -> assertThat(request.users()).containsExactly(1L, 2L),
                            () -> assertThat(request.events()).containsExactly(10L, 11L),
                            () -> assertThat(request.state()).isEqualTo(CommentState.PUBLISHED.name())
                    );
                    return true;
                }),
                argThat(paginationParams -> {
                    assertAll(
                            () -> assertThat(paginationParams.from()).isEqualTo(5),
                            () -> assertThat(paginationParams.size()).isEqualTo(5)
                    );
                    return true;
                })
        );
    }

    @Test
    void testFindAllWithoutParamsUsesDefaults() throws Exception {
        when(commentService.findAllByAdmin(any(AdminCommentSearchFilter.class), any(CommentPaginationParams.class)))
                .thenReturn(List.of());

        mockMvc.perform(get(API_PREFIX))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.length()").value(0)
                );

        verify(commentService).findAllByAdmin(
                argThat(filter -> filter.users() == null
                        && filter.events() == null
                        && (filter.state() != null && filter.state().toString().equals("ALL"))),
                argThat(paginationParams -> paginationParams.from() == 0 && paginationParams.size() == 10)
        );
    }

    @Test
    void testFindAllWithUnknownStateReturnsBadRequest() throws Exception {
        // Выполняем запрос с заведомо ложным статусом
        mockMvc.perform(get(API_PREFIX).param("state", "UNKNOWN"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(commentService);
    }

    @Test
    void testFindAllWithNegativeFromThrowsBadRequest() throws Exception {
        mockMvc.perform(get(API_PREFIX).param("from", "-1"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(commentService);
    }

    @Test
    void testFindAllWithZeroSizeThrowsBadRequest() throws Exception {
        mockMvc.perform(get(API_PREFIX).param("size", "0"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(commentService);
    }

    @Test
    void testUpdatePublish() throws Exception {
        CommentAdminUpdateDto request = new CommentAdminUpdateDto();
        request.setAdminCommentStateAction(AdminCommentStateAction.PUBLISH_COMMENT);

        when(commentService.updateByAdmin(eq(COMMENT_ID), any(CommentAdminUpdateDto.class)))
                .thenReturn(createCommentResponseDto(CommentState.PUBLISHED));

        mockMvc.perform(patch(API_PREFIX + "/{commentId}", COMMENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.id").value(COMMENT_ID),
                        jsonPath("$.state").value("PUBLISHED")
                );

        verify(commentService).updateByAdmin(eq(COMMENT_ID), argThat(req -> {
            assertThat(req.getAdminCommentStateAction()).isEqualTo(AdminCommentStateAction.PUBLISH_COMMENT);
            return true;
        }));
    }

    @Test
    void testUpdateReject() throws Exception {
        CommentAdminUpdateDto request = new CommentAdminUpdateDto();
        request.setAdminCommentStateAction(AdminCommentStateAction.REJECT_COMMENT);

        when(commentService.updateByAdmin(eq(COMMENT_ID), any(CommentAdminUpdateDto.class)))
                .thenReturn(createCommentResponseDto(CommentState.REJECTED));

        mockMvc.perform(patch(API_PREFIX + "/{commentId}", COMMENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.id").value(COMMENT_ID),
                        jsonPath("$.state").value("REJECTED")
                );

        verify(commentService).updateByAdmin(eq(COMMENT_ID), any(CommentAdminUpdateDto.class));
    }

    @Test
    void testUpdateWhenCommentNotFoundThrowsNotFound() throws Exception {
        CommentAdminUpdateDto request = new CommentAdminUpdateDto();
        request.setAdminCommentStateAction(AdminCommentStateAction.PUBLISH_COMMENT);

        when(commentService.updateByAdmin(eq(COMMENT_ID), any(CommentAdminUpdateDto.class)))
                .thenThrow(new NotFoundException("Комментарий не найден"));

        mockMvc.perform(patch(API_PREFIX + "/{commentId}", COMMENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());

        verify(commentService).updateByAdmin(eq(COMMENT_ID), any(CommentAdminUpdateDto.class));
    }

    @Test
    void testUpdateWhenConflictStateThrowsConflict() throws Exception {
        CommentAdminUpdateDto request = new CommentAdminUpdateDto();
        request.setAdminCommentStateAction(AdminCommentStateAction.PUBLISH_COMMENT);

        when(commentService.updateByAdmin(eq(COMMENT_ID), any(CommentAdminUpdateDto.class)))
                .thenThrow(new ConflictException("Комментарий можно модерировать только в состоянии PENDING"));

        mockMvc.perform(patch(API_PREFIX + "/{commentId}", COMMENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());

        verify(commentService).updateByAdmin(eq(COMMENT_ID), any(CommentAdminUpdateDto.class));
    }

    @Test
    void testUpdateWithNullActionReturnsBadRequest() throws Exception {
        CommentAdminUpdateDto request = new CommentAdminUpdateDto();

        mockMvc.perform(patch(API_PREFIX + "/{commentId}", COMMENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(commentService);
    }

    private CommentResponseDto createCommentResponseDto(CommentState state) {
        UserShortDto authorShortDto = new UserShortDto(1L, "Author Name");

        EventShortDto eventShortDto = EventShortDto.builder()
                .id(10L)
                .title("Event Title")
                .annotation("Annotation text")
                .category(new CategoryResponseDto(3L, "Category Name"))
                .confirmedRequests(0L)
                .eventDate(LocalDateTime.now().plusDays(1))
                .initiator(authorShortDto)
                .paid(false)
                .views(0L)
                .commentsCount(1L)
                .build();
        return CommentResponseDto.builder()
                .id(COMMENT_ID)
                .text(TEXT)
                .event(eventShortDto)
                .author(authorShortDto)
                .state(state)
                .createdOn(LocalDateTime.now().minusHours(1))
                .publishedOn(state == CommentState.PUBLISHED ? LocalDateTime.now() : null)
                .build();
    }
}