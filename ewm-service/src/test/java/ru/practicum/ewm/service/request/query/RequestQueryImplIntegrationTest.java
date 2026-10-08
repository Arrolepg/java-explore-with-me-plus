package ru.practicum.ewm.service.request.query;

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
import ru.practicum.ewm.service.exception.NotFoundException;
import ru.practicum.ewm.service.request.dto.ParticipationRequestDto;
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

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class RequestQueryImplIntegrationTest {

    @Autowired
    private RequestQuery requestQuery;

    @Autowired
    private RequestRepository requestRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void testFindRequests() {
        User owner = createUser("Owner", "owner@mail.com");
        User requester = createUser("Requester", "req@mail.com");
        Event event = createEvent(owner);
        createRequest(event, requester, RequestStatus.PENDING);

        List<ParticipationRequestDto> result = requestQuery.findRequests(event.getId());

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getEventId()).isEqualTo(event.getId());
        assertThat(result.getFirst().getRequesterId()).isEqualTo(requester.getId());
    }

    @Test
    void testFindRequestsEmpty() {
        List<ParticipationRequestDto> result = requestQuery.findRequests(999L);

        assertThat(result).isEmpty();
    }

    @Test
    void testFindRequestsByIdsAndEventId() {
        User owner = createUser("Owner", "owner@mail.com");
        User requester = createUser("Requester", "req@mail.com");
        Event event = createEvent(owner);
        Request request = createRequest(event, requester, RequestStatus.PENDING);

        List<Request> result = requestQuery.findRequestsByIdsAndEventId(
                List.of(request.getId()), event.getId());

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(request.getId());
    }

    @Test
    void testFindRequestsByIdsAndEventIdNotFound() {
        User owner = createUser("Owner", "owner@mail.com");
        Event event = createEvent(owner);

        assertThatThrownBy(() -> requestQuery.findRequestsByIdsAndEventId(List.of(999L), event.getId()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void testCountRequestsByEventIdAndStatus() {
        User owner = createUser("Owner", "owner@mail.com");
        User user1 = createUser("User1", "u1@mail.com");
        User user2 = createUser("User2", "u2@mail.com");
        Event event = createEvent(owner);
        createRequest(event, user1, RequestStatus.CONFIRMED);
        createRequest(event, user2, RequestStatus.CONFIRMED);

        long result = requestQuery.countRequestsByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED);

        assertThat(result).isEqualTo(2L);
    }

    @Test
    void testFindRequestsByEventIdAndStatus() {
        User owner = createUser("Owner", "owner@mail.com");
        User user1 = createUser("User1", "u1@mail.com");
        User user2 = createUser("User2", "u2@mail.com");
        Event event = createEvent(owner);
        createRequest(event, user1, RequestStatus.PENDING);
        createRequest(event, user2, RequestStatus.CONFIRMED);

        List<Request> result = requestQuery.findRequestsByEventIdAndStatus(
                event.getId(), RequestStatus.PENDING);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getStatus()).isEqualTo(RequestStatus.PENDING);
    }

    @Test
    void testCountRequestsByEventIdsAndStatus() {
        User owner = createUser("Owner", "owner@mail.com");
        User user1 = createUser("User1", "u1@mail.com");
        User user2 = createUser("User2", "u2@mail.com");
        User user3 = createUser("User3", "u3@mail.com");
        Event event1 = createEvent(owner);
        Event event2 = createEvent(owner);

        createRequest(event1, user1, RequestStatus.CONFIRMED);
        createRequest(event1, user2, RequestStatus.CONFIRMED);
        createRequest(event2, user3, RequestStatus.CONFIRMED);

        Map<Long, Long> result = requestQuery.countRequestsByEventIdsAndStatus(
                List.of(event1.getId(), event2.getId()), RequestStatus.CONFIRMED);

        assertThat(result)
                .hasSize(2)
                .containsEntry(event1.getId(), 2L)
                .containsEntry(event2.getId(), 1L);
    }

    @Test
    void testCountRequestsByEventIdsAndStatusEmpty() {
        Map<Long, Long> result = requestQuery.countRequestsByEventIdsAndStatus(
                List.of(999L), RequestStatus.CONFIRMED);

        assertThat(result).isEmpty();
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

    private Event createEvent(User initiator) {
        Category category = createCategory("Cat-" + System.nanoTime());
        Event event = Event.builder()
                .annotation("annotation")
                .category(category)
                .description("description")
                .eventDate(LocalDateTime.now().plusDays(10))
                .eventLocation(EventLocation.builder()
                        .lat(55.55f)
                        .lon(37.56f)
                        .build()
                )
                .paid(false)
                .participantLimit(10)
                .requestModeration(true)
                .title("title")
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