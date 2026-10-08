package ru.practicum.ewm.service.event.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.service.category.dto.CategoryDto;
import ru.practicum.ewm.service.event.dto.request.EventLocationCreateDto;
import ru.practicum.ewm.service.event.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.service.event.dto.request.NewEventDto;
import ru.practicum.ewm.service.event.dto.request.UpdateEventUserRequest;
import ru.practicum.ewm.service.event.dto.response.EventFullDto;
import ru.practicum.ewm.service.event.dto.response.EventRequestStatusUpdateResult;
import ru.practicum.ewm.service.event.dto.response.EventShortDto;
import ru.practicum.ewm.service.event.model.EventState;
import ru.practicum.ewm.service.event.service.EventService;
import ru.practicum.ewm.service.event.utility.PrivateEventSearchRequest;
import ru.practicum.ewm.service.event.utility.ResourceReference;
import ru.practicum.ewm.service.exception.ConflictException;
import ru.practicum.ewm.service.exception.NotFoundException;
import ru.practicum.ewm.service.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.service.request.model.RequestStatus;
import ru.practicum.ewm.service.user.dto.UserShortDto;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PrivateEventController.class)
public class PrivateEventControllerTest {
    private static final Long USER_ID = 1L;
    private static final Long EVENT_ID = 1L;
    private static final Long CATEGORY_ID = 1L;
    private static final Long REQUEST_ID = 1L;
    private static final String ANNOTATION = "Some annotation text here for event";
    private static final String DESCRIPTION = "Some description text here for event";
    private static final String TITLE = "Event title";
    private static final String API_PREFIX = "/users/{userId}/events";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EventService eventService;

    @Test
    void testFindAll() throws Exception {
        when(eventService.findAll(eq(USER_ID), any(PrivateEventSearchRequest.class)))
                .thenReturn(List.of(createEventShortDto()));

        mockMvc.perform(get(API_PREFIX, USER_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(EVENT_ID))
                .andExpect(jsonPath("$[0].title").value(TITLE));

        verify(eventService, times(1)).findAll(eq(USER_ID), any(PrivateEventSearchRequest.class));
        verifyNoMoreInteractions(eventService);
    }

    @Test
    void testFindAllEmpty() throws Exception {
        when(eventService.findAll(eq(USER_ID), any(PrivateEventSearchRequest.class)))
                .thenReturn(List.of());

        mockMvc.perform(get(API_PREFIX, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(eventService, times(1)).findAll(eq(USER_ID), any(PrivateEventSearchRequest.class));
        verifyNoMoreInteractions(eventService);
    }

    @Test
    void testFindAllWithCustomParams() throws Exception {
        when(eventService.findAll(eq(USER_ID), any(PrivateEventSearchRequest.class)))
                .thenReturn(List.of());

        mockMvc.perform(get(API_PREFIX, USER_ID)
                        .param("from", "5")
                        .param("size", "20")
                        .param("sort", "id,desc"))
                .andExpect(status().isOk());

        verify(eventService, times(1)).findAll(eq(USER_ID), any(PrivateEventSearchRequest.class));
        verifyNoMoreInteractions(eventService);
    }

    @Test
    void testFindAllUserNotFound() throws Exception {
        when(eventService.findAll(eq(99L), any(PrivateEventSearchRequest.class)))
                .thenThrow(new NotFoundException(""));

        mockMvc.perform(get(API_PREFIX, 99L))
                .andExpect(status().isNotFound());

        verify(eventService, times(1)).findAll(eq(99L), any(PrivateEventSearchRequest.class));
        verifyNoMoreInteractions(eventService);
    }

    @Test
    void testFindById() throws Exception {
        when(eventService.findById(any(ResourceReference.class)))
                .thenReturn(createEventFullDto());

        mockMvc.perform(get(API_PREFIX + "/{eventId}", USER_ID, EVENT_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(EVENT_ID))
                .andExpect(jsonPath("$.title").value(TITLE));

        verify(eventService, times(1)).findById(any(ResourceReference.class));
        verifyNoMoreInteractions(eventService);
    }

    @Test
    void testFindByIdNotFound() throws Exception {
        when(eventService.findById(any(ResourceReference.class)))
                .thenThrow(new NotFoundException(""));

        mockMvc.perform(get(API_PREFIX + "/{eventId}", USER_ID, 99L))
                .andExpect(status().isNotFound());

        verify(eventService, times(1)).findById(any(ResourceReference.class));
        verifyNoMoreInteractions(eventService);
    }

    @Test
    void testFindByIdNotInitiator() throws Exception {
        when(eventService.findById(any(ResourceReference.class)))
                .thenThrow(new ConflictException(""));

        mockMvc.perform(get(API_PREFIX + "/{eventId}", 99L, EVENT_ID))
                .andExpect(status().isConflict());

        verify(eventService, times(1)).findById(any(ResourceReference.class));
        verifyNoMoreInteractions(eventService);
    }

    @Test
    void testFindRequests() throws Exception {
        when(eventService.findRequests(any(ResourceReference.class)))
                .thenReturn(List.of(createParticipationRequestDto()));

        mockMvc.perform(get(API_PREFIX + "/{eventId}/requests", USER_ID, EVENT_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(REQUEST_ID));

        verify(eventService, times(1)).findRequests(any(ResourceReference.class));
        verifyNoMoreInteractions(eventService);
    }

    @Test
    void testFindRequestsEmpty() throws Exception {
        when(eventService.findRequests(any(ResourceReference.class)))
                .thenReturn(List.of());

        mockMvc.perform(get(API_PREFIX + "/{eventId}/requests", USER_ID, EVENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(eventService, times(1)).findRequests(any(ResourceReference.class));
        verifyNoMoreInteractions(eventService);
    }

    @Test
    void testFindRequestsNotFound() throws Exception {
        when(eventService.findRequests(any(ResourceReference.class)))
                .thenThrow(new NotFoundException(""));

        mockMvc.perform(get(API_PREFIX + "/{eventId}/requests", USER_ID, 99L))
                .andExpect(status().isNotFound());

        verify(eventService, times(1)).findRequests(any(ResourceReference.class));
        verifyNoMoreInteractions(eventService);
    }

    @Test
    void testCreate() throws Exception {
        NewEventDto req = createNewEventDto();

        when(eventService.create(eq(USER_ID), any(NewEventDto.class)))
                .thenReturn(createEventFullDto());

        mockMvc.perform(post(API_PREFIX, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(EVENT_ID))
                .andExpect(jsonPath("$.title").value(TITLE));

        verify(eventService, times(1)).create(eq(USER_ID), any(NewEventDto.class));
        verifyNoMoreInteractions(eventService);
    }

    @Test
    void testCreateUserNotFound() throws Exception {
        NewEventDto req = createNewEventDto();

        when(eventService.create(eq(99L), any(NewEventDto.class)))
                .thenThrow(new NotFoundException(""));

        mockMvc.perform(post(API_PREFIX, 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());

        verify(eventService, times(1)).create(eq(99L), any(NewEventDto.class));
        verifyNoMoreInteractions(eventService);
    }

    @Test
    void testCreateInvalidAnnotationBlank() throws Exception {
        NewEventDto req = createNewEventDto();
        req.setAnnotation("");

        mockMvc.perform(post(API_PREFIX, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(eventService);
    }

    @Test
    void testCreateInvalidAnnotationTooShort() throws Exception {
        NewEventDto req = createNewEventDto();
        req.setAnnotation("short");

        mockMvc.perform(post(API_PREFIX, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(eventService);
    }

    @Test
    void testCreateInvalidCategoryNull() throws Exception {
        NewEventDto req = createNewEventDto();
        req.setCategoryId(null);

        mockMvc.perform(post(API_PREFIX, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(eventService);
    }

    @Test
    void testCreateInvalidDescriptionBlank() throws Exception {
        NewEventDto req = createNewEventDto();
        req.setDescription("");

        mockMvc.perform(post(API_PREFIX, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(eventService);
    }

    @Test
    void testCreateInvalidEventDateNull() throws Exception {
        NewEventDto req = createNewEventDto();
        req.setEventDate(null);

        mockMvc.perform(post(API_PREFIX, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(eventService);
    }

    @Test
    void testCreateInvalidLocationNull() throws Exception {
        NewEventDto req = createNewEventDto();
        req.setLocation(null);

        mockMvc.perform(post(API_PREFIX, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(eventService);
    }

    @Test
    void testCreateInvalidTitleBlank() throws Exception {
        NewEventDto req = createNewEventDto();
        req.setTitle("");

        mockMvc.perform(post(API_PREFIX, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(eventService);
    }

    @Test
    void testCreateInvalidTitleTooShort() throws Exception {
        NewEventDto req = createNewEventDto();
        req.setTitle("ab");

        mockMvc.perform(post(API_PREFIX, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(eventService);
    }

    @Test
    void testCreateInvalidTitleTooLong() throws Exception {
        NewEventDto req = createNewEventDto();
        req.setTitle("a".repeat(121));

        mockMvc.perform(post(API_PREFIX, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(eventService);
    }

    @Test
    void testUpdate() throws Exception {
        UpdateEventUserRequest req = createUpdateEventUserRequest();

        when(eventService.update(any(ResourceReference.class), any(UpdateEventUserRequest.class)))
                .thenReturn(createEventFullDto());

        mockMvc.perform(patch(API_PREFIX + "/{eventId}", USER_ID, EVENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(EVENT_ID));

        verify(eventService, times(1)).update(any(ResourceReference.class), any(UpdateEventUserRequest.class));
        verifyNoMoreInteractions(eventService);
    }

    @Test
    void testUpdateNotFound() throws Exception {
        UpdateEventUserRequest req = createUpdateEventUserRequest();

        when(eventService.update(any(ResourceReference.class), any(UpdateEventUserRequest.class)))
                .thenThrow(new NotFoundException(""));

        mockMvc.perform(patch(API_PREFIX + "/{eventId}", USER_ID, 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());

        verify(eventService, times(1)).update(any(ResourceReference.class), any(UpdateEventUserRequest.class));
        verifyNoMoreInteractions(eventService);
    }

    @Test
    void testUpdateNotInitiator() throws Exception {
        UpdateEventUserRequest req = createUpdateEventUserRequest();

        when(eventService.update(any(ResourceReference.class), any(UpdateEventUserRequest.class)))
                .thenThrow(new ConflictException(""));

        mockMvc.perform(patch(API_PREFIX + "/{eventId}", 99L, EVENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());

        verify(eventService, times(1)).update(any(ResourceReference.class), any(UpdateEventUserRequest.class));
        verifyNoMoreInteractions(eventService);
    }

    @Test
    void testUpdateInvalidTitleTooShort() throws Exception {
        UpdateEventUserRequest req = createUpdateEventUserRequest();
        req.setTitle("ab");

        mockMvc.perform(patch(API_PREFIX + "/{eventId}", USER_ID, EVENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(eventService);
    }

    @Test
    void testUpdateInvalidAnnotationTooShort() throws Exception {
        UpdateEventUserRequest req = createUpdateEventUserRequest();
        req.setAnnotation("short");

        mockMvc.perform(patch(API_PREFIX + "/{eventId}", USER_ID, EVENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(eventService);
    }

    @Test
    void testUpdateRequests() throws Exception {
        EventRequestStatusUpdateRequest req = createStatusUpdateRequest();

        when(eventService.updateRequests(any(ResourceReference.class), any(EventRequestStatusUpdateRequest.class)))
                .thenReturn(createStatusUpdateResult());

        mockMvc.perform(patch(API_PREFIX + "/{eventId}/requests", USER_ID, EVENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.confirmedRequests.length()").value(1))
                .andExpect(jsonPath("$.rejectedRequests.length()").value(0));

        verify(eventService, times(1)).updateRequests(any(ResourceReference.class), any(EventRequestStatusUpdateRequest.class));
        verifyNoMoreInteractions(eventService);
    }

    @Test
    void testUpdateRequestsNotFound() throws Exception {
        EventRequestStatusUpdateRequest req = createStatusUpdateRequest();

        when(eventService.updateRequests(any(ResourceReference.class), any(EventRequestStatusUpdateRequest.class)))
                .thenThrow(new NotFoundException(""));

        mockMvc.perform(patch(API_PREFIX + "/{eventId}/requests", USER_ID, 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());

        verify(eventService, times(1)).updateRequests(any(ResourceReference.class), any(EventRequestStatusUpdateRequest.class));
        verifyNoMoreInteractions(eventService);
    }

    @Test
    void testUpdateRequestsConflict() throws Exception {
        EventRequestStatusUpdateRequest req = createStatusUpdateRequest();

        when(eventService.updateRequests(any(ResourceReference.class), any(EventRequestStatusUpdateRequest.class)))
                .thenThrow(new ConflictException(""));

        mockMvc.perform(patch(API_PREFIX + "/{eventId}/requests", USER_ID, EVENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());

        verify(eventService, times(1)).updateRequests(any(ResourceReference.class), any(EventRequestStatusUpdateRequest.class));
        verifyNoMoreInteractions(eventService);
    }

    private EventShortDto createEventShortDto() {
        return EventShortDto.builder()
                .id(EVENT_ID)
                .annotation(ANNOTATION)
                .category(createCategoryDto())
                .confirmedRequests(0L)
                .eventDate(LocalDateTime.now().plusDays(10))
                .initiator(createUserShortDto())
                .paid(false)
                .title(TITLE)
                .views(0L)
                .build();
    }

    private EventFullDto createEventFullDto() {
        return EventFullDto.builder()
                .id(EVENT_ID)
                .annotation(ANNOTATION)
                .category(createCategoryDto())
                .confirmedRequests(0L)
                .createdOn(LocalDateTime.now())
                .description(DESCRIPTION)
                .eventDate(LocalDateTime.now().plusDays(10))
                .initiator(createUserShortDto())
                .paid(false)
                .participantLimit(10)
                .requestModeration(true)
                .state(EventState.PENDING)
                .title(TITLE)
                .views(0L)
                .build();
    }

    private CategoryDto createCategoryDto() {
        return CategoryDto.builder()
                .id(CATEGORY_ID)
                .name("Category")
                .build();
    }

    private UserShortDto createUserShortDto() {
        return UserShortDto.builder()
                .id(USER_ID)
                .name("User")
                .build();
    }

    private ParticipationRequestDto createParticipationRequestDto() {
        return ParticipationRequestDto.builder()
                .id(REQUEST_ID)
                .created(LocalDateTime.now())
                .eventId(EVENT_ID)
                .requesterId(USER_ID)
                .status(RequestStatus.PENDING)
                .build();
    }

    private EventRequestStatusUpdateResult createStatusUpdateResult() {
        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(List.of(createParticipationRequestDto()))
                .rejectedRequests(List.of())
                .build();
    }

    private EventRequestStatusUpdateRequest createStatusUpdateRequest() {
        EventRequestStatusUpdateRequest req = new EventRequestStatusUpdateRequest();
        req.setRequestIds(List.of(REQUEST_ID));
        req.setStatus(RequestStatus.CONFIRMED);
        return req;
    }

    private NewEventDto createNewEventDto() {
        NewEventDto dto = new NewEventDto();
        dto.setAnnotation(ANNOTATION);
        dto.setCategoryId(CATEGORY_ID);
        dto.setDescription(DESCRIPTION);
        dto.setEventDate(LocalDateTime.now().plusDays(10));
        dto.setLocation(createEventLocationCreateDto());
        dto.setPaid(false);
        dto.setParticipantLimit(10);
        dto.setRequestModeration(true);
        dto.setTitle(TITLE);
        return dto;
    }

    private UpdateEventUserRequest createUpdateEventUserRequest() {
        UpdateEventUserRequest dto = new UpdateEventUserRequest();
        dto.setAnnotation(ANNOTATION);
        dto.setCategoryId(CATEGORY_ID);
        dto.setDescription(DESCRIPTION);
        dto.setEventDate(LocalDateTime.now().plusDays(10));
        dto.setPaid(false);
        dto.setParticipantLimit(10);
        dto.setRequestModeration(true);
        dto.setTitle(TITLE);
        return dto;
    }

    private EventLocationCreateDto createEventLocationCreateDto() {
        EventLocationCreateDto dto = new EventLocationCreateDto();
        dto.setLat(55.75f);
        dto.setLon(37.62f);
        return dto;
    }
}