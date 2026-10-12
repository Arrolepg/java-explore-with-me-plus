package ru.practicum.ewm.service.event.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.service.category.dto.CategoryDto;
import ru.practicum.ewm.service.event.dto.request.UpdateEventAdminRequest;
import ru.practicum.ewm.service.event.dto.response.EventFullDto;
import ru.practicum.ewm.service.event.model.AdminEventStateAction;
import ru.practicum.ewm.service.event.model.EventState;
import ru.practicum.ewm.service.event.service.EventService;
import ru.practicum.ewm.service.event.utility.AdminEventSearchFilter;
import ru.practicum.ewm.service.event.utility.PaginationParams;
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

@WebMvcTest(AdminEventController.class)
public class AdminEventControllerTest {
    private static final Long EVENT_ID = 1L;
    private static final String API_PREFIX = "/admin/events";
    private static final String TITLE = "Event title";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EventService eventService;

    @Test
    void testFindAllWithAllParams() throws Exception {
        when(eventService.findAllByAdmin(any(AdminEventSearchFilter.class), any(PaginationParams.class)))
                .thenReturn(List.of(createEventFullDto(EventState.PENDING)));

        mockMvc.perform(get(API_PREFIX)
                        .param("users", "1", "2")
                        .param("states", "PENDING", "PUBLISHED")
                        .param("categories", "3")
                        .param("rangeStart", "2030-01-01 00:00:00")
                        .param("rangeEnd", "2031-01-01 00:00:00")
                        .param("from", "5")
                        .param("size", "5"))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.length()").value(1),
                        jsonPath("$[0].id").value(EVENT_ID),
                        jsonPath("$[0].state").value("PENDING")
                );

        verify(eventService).findAllByAdmin(argThat(request -> {
            assertAll(
                    () -> assertThat(request.users()).containsExactly(1L, 2L),
                    () -> assertThat(request.states()).containsExactly(EventState.PENDING, EventState.PUBLISHED),
                    () -> assertThat(request.categories()).containsExactly(3L),
                    () -> assertThat(request.rangeStart()).isEqualTo(LocalDateTime.of(2030, 1, 1, 0, 0)),
                    () -> assertThat(request.rangeEnd()).isEqualTo(LocalDateTime.of(2031, 1, 1, 0, 0))

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
        when(eventService.findAllByAdmin(any(AdminEventSearchFilter.class), any(PaginationParams.class))).thenReturn(List.of());

        mockMvc.perform(get(API_PREFIX))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.length()").value(0)
                );

        verify(eventService).findAllByAdmin(
                argThat(filter ->
                        filter.users() == null && filter.states() == null && filter.categories() == null
                                && filter.rangeStart() == null && filter.rangeEnd() == null),
                argThat(paginationParams ->
                        paginationParams.from() == 0 && paginationParams.size() == 10
                                && paginationParams.sort() == null)
        );
    }

    @Test
    void testFindAllWithWrongDateFormatReturnsBadRequest() throws Exception {
        mockMvc.perform(get(API_PREFIX).param("rangeStart", "not-a-date"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(eventService);
    }

    @Test
    void testFindAllWithUnknownStateReturnsBadRequest() throws Exception {
        mockMvc.perform(get(API_PREFIX).param("states", "UNKNOWN"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(eventService);
    }

    @Test
    void testFindAllWithNegativeFromThrowsBadRequest() throws Exception {
        mockMvc.perform(get(API_PREFIX).param("from", "-1"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(eventService);
    }

    @Test
    void testFindAllWithZeroSizeThrowsBadRequest() throws Exception {
        mockMvc.perform(get(API_PREFIX).param("size", "0"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(eventService);
    }

    @Test
    void testUpdatePublish() throws Exception {
        UpdateEventAdminRequest request = new UpdateEventAdminRequest();
        request.setStateAction(AdminEventStateAction.PUBLISH_EVENT);
        when(eventService.updateByAdmin(eq(EVENT_ID), any(UpdateEventAdminRequest.class)))
                .thenReturn(createEventFullDto(EventState.PUBLISHED));

        mockMvc.perform(patch(API_PREFIX + "/{eventId}", EVENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.id").value(EVENT_ID),
                        jsonPath("$.state").value("PUBLISHED")
                );

        verify(eventService).updateByAdmin(eq(EVENT_ID), any(UpdateEventAdminRequest.class));
    }

    @Test
    void testUpdateWithShortTitleReturnsBadRequest() throws Exception {
        mockMvc.perform(patch(API_PREFIX + "/{eventId}", EVENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"ab\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(eventService);
    }

    @Test
    void testUpdateWithPastEventDateReturnsBadRequest() throws Exception {
        mockMvc.perform(patch(API_PREFIX + "/{eventId}", EVENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventDate\":\"2000-01-01 10:00:00\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(eventService);
    }

    @Test
    void testUpdateNotFound() throws Exception {
        when(eventService.updateByAdmin(eq(EVENT_ID), any(UpdateEventAdminRequest.class)))
                .thenThrow(new NotFoundException("Событие с id = 1 не найдено"));

        mockMvc.perform(patch(API_PREFIX + "/{eventId}", EVENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpdateConflict() throws Exception {
        when(eventService.updateByAdmin(eq(EVENT_ID), any(UpdateEventAdminRequest.class)))
                .thenThrow(new ConflictException("Нельзя опубликовать событие"));

        mockMvc.perform(patch(API_PREFIX + "/{eventId}", EVENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stateAction\":\"PUBLISH_EVENT\"}"))
                .andExpect(status().isConflict());
    }

    private EventFullDto createEventFullDto(EventState state) {
        return EventFullDto.builder()
                .id(EVENT_ID)
                .annotation("Some annotation text here for event")
                .category(CategoryDto.builder().id(1L).name("Category").build())
                .description("Some description text here for event")
                .eventDate(LocalDateTime.now().plusDays(10))
                .initiator(UserShortDto.builder().id(1L).name("User").build())
                .paid(false)
                .participantLimit(0)
                .requestModeration(true)
                .state(state)
                .title(TITLE)
                .build();
    }
}
