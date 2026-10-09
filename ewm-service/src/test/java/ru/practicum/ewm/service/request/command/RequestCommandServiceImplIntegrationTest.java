package ru.practicum.ewm.service.request.command;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.service.category.model.Category;
import ru.practicum.ewm.service.category.repository.CategoryRepository;
import ru.practicum.ewm.service.event.model.Event;
import ru.practicum.ewm.service.event.model.EventLocation;
import ru.practicum.ewm.service.event.model.EventState;
import ru.practicum.ewm.service.event.repository.EventRepository;
import ru.practicum.ewm.service.exception.ConflictException;
import ru.practicum.ewm.service.exception.NotFoundException;
import ru.practicum.ewm.service.request.model.Request;
import ru.practicum.ewm.service.request.model.RequestStatus;
import ru.practicum.ewm.service.request.repository.RequestRepository;
import ru.practicum.ewm.service.user.model.User;
import ru.practicum.ewm.service.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class RequestCommandServiceImplIntegrationTest {
    private static final String OWNER_NAME = "Owner";
    private static final String OWNER_EMAIL = "owner@mail.com";
    private static final String SECOND_NAME = "Second";
    private static final String SECOND_EMAIL = "second@mail.com";

    @Autowired
    private RequestCommandService requestCommandService;

    @Autowired
    private RequestRepository requestRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void testUpdateRequestsStatusesConfirmed() {
        User owner = createUser(OWNER_NAME, OWNER_EMAIL);
        User requester = createUser(SECOND_NAME, SECOND_EMAIL);
        Event event = createEvent(owner, 10);
        Request request = createRequest(event, requester, RequestStatus.PENDING);

        RequestStatusUpdateCommand command = new RequestStatusUpdateCommand(
                event.getId(), List.of(request.getId()), RequestStatus.CONFIRMED, 10);

        RequestStatusUpdateResult result = requestCommandService.updateRequestsStatuses(command);

        assertThat(result.confirmedRequests()).hasSize(1);
        assertThat(result.rejectedRequests()).isEmpty();

        Request updated = requestRepository.findById(request.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(RequestStatus.CONFIRMED);
    }

    @Test
    void testUpdateRequestsStatusesRejected() {
        User owner = createUser(OWNER_NAME, OWNER_EMAIL);
        User requester = createUser(SECOND_NAME, SECOND_EMAIL);
        Event event = createEvent(owner, 10);
        Request request = createRequest(event, requester, RequestStatus.PENDING);

        RequestStatusUpdateCommand command = new RequestStatusUpdateCommand(
                event.getId(), List.of(request.getId()), RequestStatus.REJECTED, 10);

        RequestStatusUpdateResult result = requestCommandService.updateRequestsStatuses(command);

        assertThat(result.rejectedRequests()).hasSize(1);
        assertThat(result.confirmedRequests()).isEmpty();

        Request updated = requestRepository.findById(request.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(RequestStatus.REJECTED);
    }

    @Test
    void testUpdateRequestsStatusesRequestNotFound() {
        User owner = createUser(OWNER_NAME, OWNER_EMAIL);
        Event event = createEvent(owner, 10);

        RequestStatusUpdateCommand command = new RequestStatusUpdateCommand(
                event.getId(), List.of(999L), RequestStatus.CONFIRMED, 10);

        assertThatThrownBy(() -> requestCommandService.updateRequestsStatuses(command))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void testUpdateRequestsStatusesNotPending() {
        User owner = createUser(OWNER_NAME, OWNER_EMAIL);
        User requester = createUser(SECOND_NAME, SECOND_EMAIL);
        Event event = createEvent(owner, 10);
        Request request = createRequest(event, requester, RequestStatus.CONFIRMED);

        RequestStatusUpdateCommand command = new RequestStatusUpdateCommand(
                event.getId(), List.of(request.getId()), RequestStatus.CONFIRMED, 10);

        assertThatThrownBy(() -> requestCommandService.updateRequestsStatuses(command))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void testUpdateRequestsStatusesLimitReached() {
        User owner = createUser(OWNER_NAME, OWNER_EMAIL);
        User req1 = createUser(SECOND_NAME, SECOND_EMAIL);
        User req2 = createUser("Req2", "req2@mail.com");

        Event event = createEvent(owner, 1);
        createRequest(event, req1, RequestStatus.CONFIRMED);
        Request pending = createRequest(event, req2, RequestStatus.PENDING);

        RequestStatusUpdateCommand command = new RequestStatusUpdateCommand(
                event.getId(), List.of(pending.getId()), RequestStatus.CONFIRMED, 1);

        assertThatThrownBy(() -> requestCommandService.updateRequestsStatuses(command))
                .isInstanceOf(ConflictException.class);

        Request notUpdated = requestRepository.findById(pending.getId()).orElseThrow();
        assertThat(notUpdated.getStatus()).isEqualTo(RequestStatus.PENDING);
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

    private Event createEvent(User initiator, int participantLimit) {
        Category category = createCategory("Cat-" + System.nanoTime());
        Event event = Event.builder()
                .annotation("Annotation")
                .category(category)
                .description("Description")
                .eventDate(LocalDateTime.now().plusDays(10))
                .eventLocation(EventLocation.builder().lat(55.75f).lon(37.62f).build())
                .paid(false)
                .participantLimit(participantLimit)
                .requestModeration(true)
                .title("Title")
                .state(EventState.PENDING)
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
}