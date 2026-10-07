package ru.practicum.ewm.service.request.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.ewm.service.category.model.Category;
import ru.practicum.ewm.service.event.model.Event;
import ru.practicum.ewm.service.event.model.EventLocation;
import ru.practicum.ewm.service.event.model.EventState;
import ru.practicum.ewm.service.request.model.Request;
import ru.practicum.ewm.service.request.model.RequestStatus;
import ru.practicum.ewm.service.request.repository.projection.EventRequestsCount;
import ru.practicum.ewm.service.user.model.User;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@ActiveProfiles("test")
public class RequestRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private RequestRepository requestRepository;

    @Test
    void testCountRequestsByEventIdsAndStatus() {
        User owner = createUser("Owner", "owner@mail.com");
        User user1 = createUser("User1", "u1@mail.com");
        User user2 = createUser("User2", "u2@mail.com");
        Category category = createCategory("Cat");
        Event event = createEvent(owner, category);

        createRequest(event, user1, RequestStatus.CONFIRMED);
        createRequest(event, user2, RequestStatus.CONFIRMED);

        List<EventRequestsCount> result = requestRepository
                .countRequestsByEventIdsAndStatus(List.of(event.getId()), RequestStatus.CONFIRMED);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().eventId()).isEqualTo(event.getId());
        assertThat(result.getFirst().count()).isEqualTo(2L);
    }

    @Test
    void testCountRequestsByEventIdsAndStatusFiltersByStatus() {
        User owner = createUser("Owner", "owner@mail.com");
        User user1 = createUser("User1", "u1@mail.com");
        User user2 = createUser("User2", "u2@mail.com");
        Category category = createCategory("Cat");
        Event event = createEvent(owner, category);

        createRequest(event, user1, RequestStatus.CONFIRMED);
        createRequest(event, user2, RequestStatus.PENDING);

        List<EventRequestsCount> result = requestRepository
                .countRequestsByEventIdsAndStatus(List.of(event.getId()), RequestStatus.CONFIRMED);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().count()).isEqualTo(1L);
    }

    @Test
    void testCountRequestsByEventIdsAndStatusGroupsByEvent() {
        User owner = createUser("Owner", "owner@mail.com");
        User user1 = createUser("User1", "u1@mail.com");
        User user2 = createUser("User2", "u2@mail.com");
        User user3 = createUser("User3", "u3@mail.com");
        Category category = createCategory("Cat");
        Event event1 = createEvent(owner, category);
        Event event2 = createEvent(owner, category);

        createRequest(event1, user1, RequestStatus.CONFIRMED);
        createRequest(event1, user2, RequestStatus.CONFIRMED);
        createRequest(event2, user3, RequestStatus.CONFIRMED);

        List<EventRequestsCount> result = requestRepository
                .countRequestsByEventIdsAndStatus(
                        List.of(event1.getId(), event2.getId()), RequestStatus.CONFIRMED);

        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(EventRequestsCount::eventId, EventRequestsCount::count)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(event1.getId(), 2L),
                        org.assertj.core.groups.Tuple.tuple(event2.getId(), 1L));
    }

    @Test
    void testCountRequestsByEventIdsAndStatusEmpty() {
        List<EventRequestsCount> result = requestRepository
                .countRequestsByEventIdsAndStatus(List.of(999L), RequestStatus.CONFIRMED);

        assertThat(result).isEmpty();
    }

    private User createUser(String name, String email) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        return entityManager.persistAndFlush(user);
    }

    private Category createCategory(String name) {
        Category category = new Category();
        category.setName(name);
        return entityManager.persistAndFlush(category);
    }

    private Event createEvent(User initiator, Category category) {
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
        return entityManager.persistAndFlush(event);
    }

    private void createRequest(Event event, User requester, RequestStatus status) {
        Request request = Request.builder()
                .event(event)
                .requester(requester)
                .status(status)
                .created(LocalDateTime.now())
                .build();
        entityManager.persistAndFlush(request);
    }
}