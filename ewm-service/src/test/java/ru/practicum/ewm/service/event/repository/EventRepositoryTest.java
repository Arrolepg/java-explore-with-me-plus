package ru.practicum.ewm.service.event.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.ewm.service.category.model.Category;
import ru.practicum.ewm.service.event.model.Event;
import ru.practicum.ewm.service.event.model.EventLocation;
import ru.practicum.ewm.service.event.model.EventState;
import ru.practicum.ewm.service.user.model.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@ActiveProfiles("test")
public class EventRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private EventRepository eventRepository;

    @Test
    void testFindByIdWithFetch() {
        User owner = createUser("Owner", "owner@mail.com");
        Category category = createCategory("Cat");
        Event event = createEvent(owner, category, "Title");

        Optional<Event> result = eventRepository.findByIdWithFetch(event.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(event.getId());
        assertThat(result.get().getCategory().getId()).isEqualTo(category.getId());
        assertThat(result.get().getInitiator().getId()).isEqualTo(owner.getId());
    }

    @Test
    void testFindByIdWithFetchNotFound() {
        assertThat(eventRepository.findByIdWithFetch(999L)).isEmpty();
    }

    @Test
    void testFindEventsByInitiatorIdWithFetch() {
        User owner = createUser("Owner", "owner@mail.com");
        User other = createUser("Other", "other@mail.com");
        Category category = createCategory("Cat");

        createEvent(owner, category, "Event 1");
        createEvent(owner, category, "Event 2");
        createEvent(other, category, "Event 3");

        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "id"));

        List<Event> result = eventRepository.findEventsByInitiatorIdWithFetch(owner.getId(), pageable);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Event::getTitle)
                .containsExactlyInAnyOrder("Event 1", "Event 2");
    }

    @Test
    void testFindEventsByInitiatorIdWithFetchEmpty() {
        User owner = createUser("Owner", "owner@mail.com");

        Pageable pageable = PageRequest.of(0, 10);

        List<Event> result = eventRepository.findEventsByInitiatorIdWithFetch(owner.getId(), pageable);

        assertThat(result).isEmpty();
    }

    @Test
    void testFindEventsByInitiatorIdWithFetchPagination() {
        User owner = createUser("Owner", "owner@mail.com");
        Category category = createCategory("Cat");

        for (int i = 0; i < 5; i++) {
            createEvent(owner, category, "Event " + i);
        }

        Pageable firstPage = PageRequest.of(0, 2, Sort.by(Sort.Direction.ASC, "id"));
        Pageable secondPage = PageRequest.of(1, 2, Sort.by(Sort.Direction.ASC, "id"));

        List<Event> page1 = eventRepository.findEventsByInitiatorIdWithFetch(owner.getId(), firstPage);
        List<Event> page2 = eventRepository.findEventsByInitiatorIdWithFetch(owner.getId(), secondPage);

        assertThat(page1).hasSize(2);
        assertThat(page2).hasSize(2);
        assertThat(page1.getFirst().getId()).isNotEqualTo(page2.getFirst().getId());
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

    private Event createEvent(User initiator, Category category, String title) {
        Event event = Event.builder()
                .annotation("Some annotation text here for event")
                .category(category)
                .description("Some description text here for event")
                .eventDate(LocalDateTime.now().plusDays(10))
                .eventLocation(EventLocation.builder().lat(55.75f).lon(37.62f).build())
                .paid(false)
                .participantLimit(10)
                .requestModeration(true)
                .title(title)
                .state(EventState.PENDING)
                .createdOn(LocalDateTime.now())
                .initiator(initiator)
                .build();
        return entityManager.persistAndFlush(event);
    }
}