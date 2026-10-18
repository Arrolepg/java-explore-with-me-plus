package ru.practicum.ewm.service.compilation.repository;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.ewm.service.category.model.Category;
import ru.practicum.ewm.service.compilation.model.Compilation;
import ru.practicum.ewm.service.compilation.utility.CompilationPaginationParams;
import ru.practicum.ewm.service.event.model.Event;
import ru.practicum.ewm.service.event.model.EventLocation;
import ru.practicum.ewm.service.event.model.EventState;
import ru.practicum.ewm.service.user.model.User;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@ActiveProfiles("test")
public class CompilationRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CompilationRepository compilationRepository;

    @Test
    void findCompilationsShouldReturnBothPinnedAndUnpinnedWhenPinnedIsNull() {
        Compilation comp1 = createComp(List.of(),false, "Спектакли");
        Compilation comp2 = createComp(List.of(),true, "Концерты");
        CompilationPaginationParams params = getParams(0, 3);

        List<Compilation> compilations = compilationRepository.findCompilations(null, params);

        assertThat(compilations)
                .extracting(Compilation::getId)
                .containsExactlyInAnyOrder(comp1.getId(), comp2.getId());
    }

    @Test
    void findCompilationsShouldReturnEmptyList() {
        CompilationPaginationParams params = getParams(0, 5);

        List<Compilation> compilations = compilationRepository.findCompilations(null, params);

        assertThat(compilations).isEmpty();
    }

    @Test
    void findCompilationsShouldReturnPinnedWhenPinnedIsTrue() {
        createComp(List.of(),false, "Спектакли");
        Compilation comp2 = createComp(List.of(),true, "Концерты");
        CompilationPaginationParams params = getParams(0, 5);

        List<Compilation> compilations = compilationRepository.findCompilations(true, params);

        assertThat(compilations)
                .extracting(Compilation::getId)
                .containsExactlyInAnyOrder(comp2.getId());
    }

    @Test
    void findCompilationsShouldReturnSecondCompilation() {
        createComp(List.of(),false, "Спектакли");
        Compilation comp2 = createComp(List.of(),false, "Концерты");
        createComp(List.of(),false, "Выставки");

        CompilationPaginationParams params = getParams(1, 1);

        List<Compilation> compilations = compilationRepository.findCompilations(false, params);

        assertThat(compilations)
                .extracting(Compilation::getId)
                .containsExactlyInAnyOrder(comp2.getId());
    }

    @Test
    void findAllWithEventsByIdInShouldInitializeEventsOfManagedCompilation() {
        User owner = createUser();
        Category category = createCategory();
        Event event1 = createEvent(owner, category);
        Event event2 = createEvent(owner, category);
        createComp(List.of(event1, event2),false, "Спектакли");

        entityManager.flush();
        entityManager.clear();

        CompilationPaginationParams params = getParams(0, 5);
        List<Compilation> compilations = compilationRepository.findCompilations(null, params);

        assertThat(compilations).hasSize(1);

        Compilation compilation = compilations.getFirst();

        assertThat(Hibernate.isInitialized(compilation.getEvents())).isFalse();

        compilationRepository.findAllWithEventsByIdIn(List.of(compilation.getId()));

        assertThat(Hibernate.isInitialized(compilation.getEvents())).isTrue();

        assertThat(compilation.getEvents())
                .extracting(Event::getId)
                .containsExactlyInAnyOrder(event1.getId(), event2.getId());

        for (Event event : compilation.getEvents()) {
            assertThat(Hibernate.isInitialized(event.getCategory())).isTrue();
            assertThat(Hibernate.isInitialized(event.getInitiator())).isTrue();
        }
    }

    @Test
    void findByIdWithEventsLoadsEventsWithCategoryAndInitiator() {
        User owner = createUser();
        Category category = createCategory();
        Event event1 = createEvent(owner, category);
        Event event2 = createEvent(owner, category);

        Compilation compilation =
                createComp(List.of(event1, event2), false, "Concerts");

        Long compId = compilation.getId();
        List<Long> eventIds = List.of(event1.getId(), event2.getId());

        entityManager.flush();
        entityManager.clear();

        var result = compilationRepository.findByIdWithEvents(compId);

        assertThat(result).isPresent();

        Compilation loaded = result.orElseThrow();

        assertThat(loaded.getId()).isEqualTo(compId);
        assertThat(Hibernate.isInitialized(loaded.getEvents())).isTrue();

        assertThat(loaded.getEvents())
                .extracting(Event::getId)
                .containsExactlyInAnyOrderElementsOf(eventIds);

        for (Event event : loaded.getEvents()) {
            assertThat(Hibernate.isInitialized(event.getCategory())).isTrue();
            assertThat(Hibernate.isInitialized(event.getInitiator())).isTrue();
        }
    }

    @Test
    void findByIdWithEventsReturnsCompilationWithoutEvents() {
        Compilation compilation = createComp(List.of(), false, "Concerts");
        Long compId = compilation.getId();

        entityManager.flush();
        entityManager.clear();

        var result = compilationRepository.findByIdWithEvents(compId);

        assertThat(result).isPresent();

        Compilation loaded = result.orElseThrow();

        assertThat(loaded.getId()).isEqualTo(compId);
        assertThat(Hibernate.isInitialized(loaded.getEvents())).isTrue();
        assertThat(loaded.getEvents()).isEmpty();
    }

    @Test
    void findByIdWithEventsReturnsEmptyWhenCompilationDoesNotExist() {
        assertThat(compilationRepository.findByIdWithEvents(-1L)).isEmpty();
    }

    private User createUser() {
        User user = new User();
        user.setName("Owner");
        user.setEmail("owner@mail.com");
        return entityManager.persistAndFlush(user);
    }

    private Category createCategory() {
        Category category = new Category();
        category.setName("category");
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

    private Compilation createComp(List<Event> events, Boolean pinned, String title) {
        return entityManager.persistAndFlush(
                Compilation.builder()
                    .events(events)
                    .pinned(pinned)
                    .title(title)
                    .build()
        );
    }

    private CompilationPaginationParams getParams(Integer from, Integer size) {
        return CompilationPaginationParams.builder()
                .from(from)
                .size(size)
                .build();
    }
}
