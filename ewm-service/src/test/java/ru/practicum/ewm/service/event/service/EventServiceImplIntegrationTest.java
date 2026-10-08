package ru.practicum.ewm.service.event.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.service.category.model.Category;
import ru.practicum.ewm.service.category.repository.CategoryRepository;
import ru.practicum.ewm.service.event.dto.request.EventLocationCreateDto;
import ru.practicum.ewm.service.event.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.service.event.dto.request.NewEventDto;
import ru.practicum.ewm.service.event.dto.request.UpdateEventUserRequest;
import ru.practicum.ewm.service.event.dto.response.EventFullDto;
import ru.practicum.ewm.service.event.dto.response.EventRequestStatusUpdateResult;
import ru.practicum.ewm.service.event.dto.response.EventShortDto;
import ru.practicum.ewm.service.event.model.Event;
import ru.practicum.ewm.service.event.model.EventLocation;
import ru.practicum.ewm.service.event.model.EventState;
import ru.practicum.ewm.service.event.repository.EventRepository;
import ru.practicum.ewm.service.event.utility.PrivateEventSearchRequest;
import ru.practicum.ewm.service.adapter.EventStatsAdapter;
import ru.practicum.ewm.service.event.utility.ResourceReference;
import ru.practicum.ewm.service.exception.ConflictException;
import ru.practicum.ewm.service.exception.NotFoundException;
import ru.practicum.ewm.service.request.model.Request;
import ru.practicum.ewm.service.request.model.RequestStatus;
import ru.practicum.ewm.service.request.repository.RequestRepository;
import ru.practicum.ewm.service.user.model.User;
import ru.practicum.ewm.service.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class EventServiceImplIntegrationTest {
    private static final String ANNOTATION = "Annotation";
    private static final String DESCRIPTION = "Description";
    private static final String TITLE = "Title";

    @Autowired
    private EventService eventService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private RequestRepository requestRepository;

    @MockBean
    private EventStatsAdapter eventStatsAdapter;

    @Test
    void testCreate() {
        User owner = createUser("Owner", "owner@mail.com");
        Category category = createCategory("Cat");

        EventFullDto result = eventService.create(owner.getId(), createNewEventDto(category.getId()));

        assertThat(result.getId()).isNotNull();
        assertThat(result.getTitle()).isEqualTo(TITLE);
        assertThat(eventRepository.findById(result.getId())).isPresent();
    }

    @Test
    void testCreateUserNotFound() {
        assertThatThrownBy(() -> eventService.create(999L, createNewEventDto(1L)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void testCreateCategoryNotFound() {
        User owner = createUser("Owner", "owner@mail.com");

        assertThatThrownBy(() -> eventService.create(owner.getId(), createNewEventDto(999L)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void testUpdate() {
        User owner = createUser("Owner", "owner@mail.com");
        Event event = createEvent(owner, EventState.PENDING);
        when(eventStatsAdapter.getViews(any(Event.class))).thenReturn(0L);

        UpdateEventUserRequest dto = new UpdateEventUserRequest();
        dto.setTitle("Updated title");

        EventFullDto result = eventService.update(new ResourceReference(owner.getId(), event.getId()), dto);

        assertThat(result.getTitle()).isEqualTo("Updated title");
    }

    @Test
    void testUpdateUserNotFound() {
        assertThatThrownBy(() -> eventService.update(
                new ResourceReference(999L, 1L), new UpdateEventUserRequest()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void testUpdateEventNotFound() {
        User owner = createUser("Owner", "owner@mail.com");

        assertThatThrownBy(() -> eventService.update(
                new ResourceReference(owner.getId(), 999L), new UpdateEventUserRequest()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void testUpdateNotInitiator() {
        User owner = createUser("Owner", "owner@mail.com");
        User stranger = createUser("Stranger", "stranger@mail.com");
        Event event = createEvent(owner, EventState.PENDING);

        assertThatThrownBy(() -> eventService.update(
                new ResourceReference(stranger.getId(), event.getId()), new UpdateEventUserRequest()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void testUpdatePublishedState() {
        User owner = createUser("Owner", "owner@mail.com");
        Event event = createEvent(owner, EventState.PUBLISHED);

        assertThatThrownBy(() -> eventService.update(
                new ResourceReference(owner.getId(), event.getId()), new UpdateEventUserRequest()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void testFindRequests() {
        User owner = createUser("Owner", "owner@mail.com");
        User requester = createUser("Requester", "req@mail.com");
        Event event = createEvent(owner, EventState.PENDING);
        createRequest(event, requester, RequestStatus.PENDING);

        var result = eventService.findRequests(new ResourceReference(owner.getId(), event.getId()));

        assertThat(result).hasSize(1);
    }

    @Test
    void testFindRequestsEventNotFound() {
        User owner = createUser("Owner", "owner@mail.com");

        assertThatThrownBy(() -> eventService.findRequests(
                new ResourceReference(owner.getId(), 999L)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void testUpdateRequestsConfirmed() {
        User owner = createUser("Owner", "owner@mail.com");
        User requester = createUser("Requester", "req@mail.com");
        Event event = createEvent(owner, EventState.PENDING);
        event.setParticipantLimit(10);
        eventRepository.save(event);
        Request request = createRequest(event, requester, RequestStatus.PENDING);

        EventRequestStatusUpdateRequest dto = new EventRequestStatusUpdateRequest();
        dto.setRequestIds(List.of(request.getId()));
        dto.setStatus(RequestStatus.CONFIRMED);

        EventRequestStatusUpdateResult result = eventService.updateRequests(
                new ResourceReference(owner.getId(), event.getId()), dto);

        assertThat(result.getConfirmedRequests()).hasSize(1);
        assertThat(result.getRejectedRequests()).isEmpty();
    }

    @Test
    void testUpdateRequestsRejected() {
        User owner = createUser("Owner", "owner@mail.com");
        User requester = createUser("Requester", "req@mail.com");
        Event event = createEvent(owner, EventState.PENDING);
        Request request = createRequest(event, requester, RequestStatus.PENDING);

        EventRequestStatusUpdateRequest dto = new EventRequestStatusUpdateRequest();
        dto.setRequestIds(List.of(request.getId()));
        dto.setStatus(RequestStatus.REJECTED);

        EventRequestStatusUpdateResult result = eventService.updateRequests(
                new ResourceReference(owner.getId(), event.getId()), dto);

        assertThat(result.getRejectedRequests()).hasSize(1);
        assertThat(result.getConfirmedRequests()).isEmpty();
    }

    @Test
    void testUpdateRequestsNotPending() {
        User owner = createUser("Owner", "owner@mail.com");
        User requester = createUser("Requester", "req@mail.com");
        Event event = createEvent(owner, EventState.PENDING);
        Request request = createRequest(event, requester, RequestStatus.CONFIRMED);

        EventRequestStatusUpdateRequest dto = new EventRequestStatusUpdateRequest();
        dto.setRequestIds(List.of(request.getId()));
        dto.setStatus(RequestStatus.CONFIRMED);

        assertThatThrownBy(() -> eventService.updateRequests(
                new ResourceReference(owner.getId(), event.getId()), dto))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void testFindAll() {
        User owner = createUser("Owner", "owner@mail.com");
        createEvent(owner, EventState.PENDING);
        createEvent(owner, EventState.PENDING);
        when(eventStatsAdapter.getViews(anyList())).thenReturn(Map.of());

        List<EventShortDto> result = eventService.findAll(owner.getId(),
                new PrivateEventSearchRequest(0, 10));

        assertThat(result).hasSize(2);
    }

    @Test
    void testFindAllEmpty() {
        User owner = createUser("Owner", "owner@mail.com");

        List<EventShortDto> result = eventService.findAll(owner.getId(),
                new PrivateEventSearchRequest(0, 10));

        assertThat(result).isEmpty();
    }

    @Test
    void testFindAllUserNotFound() {
        assertThatThrownBy(() -> eventService.findAll(999L,
                new PrivateEventSearchRequest(0, 10)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void testFindById() {
        User owner = createUser("Owner", "owner@mail.com");
        Event event = createEvent(owner, EventState.PENDING);
        when(eventStatsAdapter.getViews(any(Event.class))).thenReturn(0L);

        EventFullDto result = eventService.findById(new ResourceReference(owner.getId(), event.getId()));

        assertThat(result.getId()).isEqualTo(event.getId());
    }

    @Test
    void testFindByIdEventNotFound() {
        User owner = createUser("Owner", "owner@mail.com");

        assertThatThrownBy(() -> eventService.findById(
                new ResourceReference(owner.getId(), 999L)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void testFindByIdNotInitiator() {
        User owner = createUser("Owner", "owner@mail.com");
        User stranger = createUser("Stranger", "stranger@mail.com");
        Event event = createEvent(owner, EventState.PENDING);

        assertThatThrownBy(() -> eventService.findById(
                new ResourceReference(stranger.getId(), event.getId())))
                .isInstanceOf(ConflictException.class);
    }

    private User createUser(String name, String email) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        return userRepository.save(user);
    }

    private Category createCategory(String name) {
        Category category = new Category();
        category.setName(name);
        return categoryRepository.save(category);
    }

    private Event createEvent(User initiator, EventState state) {
        Category category = createCategory("Cat-" + System.nanoTime());
        Event event = Event.builder()
                .annotation(ANNOTATION)
                .category(category)
                .description(DESCRIPTION)
                .eventDate(LocalDateTime.now().plusDays(10))
                .eventLocation(EventLocation.builder().lat(55.75f).lon(37.62f).build())
                .paid(false)
                .participantLimit(10)
                .requestModeration(true)
                .title(TITLE)
                .state(state)
                .createdOn(LocalDateTime.now())
                .initiator(initiator)
                .build();
        return eventRepository.save(event);
    }

    private Request createRequest(Event event, User requester, RequestStatus status) {
        Request request = Request.builder()
                .event(event)
                .requester(requester)
                .status(status)
                .created(LocalDateTime.now())
                .build();
        return requestRepository.save(request);
    }

    private NewEventDto createNewEventDto(Long categoryId) {
        NewEventDto dto = new NewEventDto();
        dto.setAnnotation(ANNOTATION);
        dto.setCategoryId(categoryId);
        dto.setDescription(DESCRIPTION);
        dto.setEventDate(LocalDateTime.now().plusDays(10));
        EventLocationCreateDto location = new EventLocationCreateDto();
        location.setLat(55.75f);
        location.setLon(37.62f);
        dto.setLocation(location);
        dto.setPaid(false);
        dto.setParticipantLimit(10);
        dto.setRequestModeration(true);
        dto.setTitle(TITLE);
        return dto;
    }
}