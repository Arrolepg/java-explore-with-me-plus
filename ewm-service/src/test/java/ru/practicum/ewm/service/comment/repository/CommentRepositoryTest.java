package ru.practicum.ewm.service.comment.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.ewm.service.category.model.Category;
import ru.practicum.ewm.service.comment.model.Comment;
import ru.practicum.ewm.service.comment.model.CommentState;
import ru.practicum.ewm.service.comment.repository.projection.EventCommentsCount;
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
public class CommentRepositoryTest {
    private static final String NAME = "Author";

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CommentRepository commentRepository;

    @Test
    void countCommentsByEventIdsAndStateCountsOnlyPublished() {
        User author = createUser("1");
        User author2 = createUser("2");
        User author3 = createUser("3");
        Category category = createCategory();
        Event event1 = createEvent(author, category, "Event 1");
        Event event2 = createEvent(author, category, "Event 2");

        createComment(author, event1, CommentState.PUBLISHED);
        createComment(author2, event1, CommentState.PUBLISHED);
        createComment(author3, event1, CommentState.PENDING);
        createComment(author, event2, CommentState.PUBLISHED);
        createComment(author3, event2, CommentState.REJECTED);

        entityManager.flush();
        entityManager.clear();

        List<EventCommentsCount> result = commentRepository.countCommentsByEventIdsAndState(
                List.of(event1.getId(), event2.getId()), CommentState.PUBLISHED);

        assertThat(result).hasSize(2);

        EventCommentsCount forEvent1 = findById(result, event1.getId());
        EventCommentsCount forEvent2 = findById(result, event2.getId());

        assertThat(forEvent1.commentsCount()).isEqualTo(2L);
        assertThat(forEvent2.commentsCount()).isEqualTo(1L);
    }

    @Test
    void countCommentsByEventIdsAndStateReturnsEmptyWhenNoPublished() {
        User author = createUser("1");
        User author2 = createUser("2");
        Category category = createCategory();
        Event event = createEvent(author, category, "Event");

        createComment(author, event, CommentState.PENDING);
        createComment(author2, event, CommentState.PENDING);

        entityManager.flush();
        entityManager.clear();

        List<EventCommentsCount> result = commentRepository.countCommentsByEventIdsAndState(
                List.of(event.getId()), CommentState.PUBLISHED);

        assertThat(result).isEmpty();
    }

    @Test
    void countCommentsByEventIdsAndStateIgnoresOtherEvents() {
        User author = createUser("1");
        User author2 = createUser("2");
        Category category = createCategory();
        Event event1 = createEvent(author, category, "Event 1");
        Event event2 = createEvent(author, category, "Event 2");

        createComment(author, event1, CommentState.PUBLISHED);
        createComment(author, event2, CommentState.PUBLISHED);
        createComment(author2, event2, CommentState.PUBLISHED);

        entityManager.flush();
        entityManager.clear();

        List<EventCommentsCount> result = commentRepository.countCommentsByEventIdsAndState(
                List.of(event1.getId()), CommentState.PUBLISHED);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().eventId()).isEqualTo(event1.getId());
        assertThat(result.getFirst().commentsCount()).isEqualTo(1L);
    }

    @Test
    void findCommentsByEventIdAndStateWithAuthorFetchReturnsOnlyPublished() {
        User author = createUser("1");
        User author2 = createUser("2");
        User author3 = createUser("3");
        Category category = createCategory();
        Event event = createEvent(author, category, "Event");

        createComment(author, event, CommentState.PUBLISHED);
        createComment(author2, event, CommentState.PUBLISHED);
        createComment(author3, event, CommentState.PENDING);

        entityManager.flush();
        entityManager.clear();

        List<Comment> result = commentRepository.findCommentsByEventIdAndStateWithAuthorFetch(
                event.getId(), CommentState.PUBLISHED);

        assertThat(result).hasSize(2);
        assertThat(result).allSatisfy(c -> {
            assertThat(c.getEvent().getId()).isEqualTo(event.getId());
            assertThat(c.getState()).isEqualTo(CommentState.PUBLISHED);
            assertThat(c.getAuthor().getName()).isNotNull();
        });
    }

    @Test
    void findCommentsByEventIdAndStateWithAuthorFetchReturnsEmpty() {
        User author = createUser("1");
        Category category = createCategory();
        Event event = createEvent(author, category, "Event");

        entityManager.flush();
        entityManager.clear();

        List<Comment> result = commentRepository.findCommentsByEventIdAndStateWithAuthorFetch(
                event.getId(), CommentState.PUBLISHED);

        assertThat(result).isEmpty();
    }

    @Test
    void findCommentsByEventIdsAndStateWithAuthorFetchReturnsCommentsOfAllEvents() {
        User author = createUser("1");
        User author2 = createUser("2");
        User author3 = createUser("3");
        Category category = createCategory();
        Event event1 = createEvent(author, category, "Event 1");
        Event event2 = createEvent(author, category, "Event 2");

        createComment(author, event1, CommentState.PUBLISHED);
        createComment(author2, event1, CommentState.PUBLISHED);
        createComment(author3, event1, CommentState.PENDING);
        createComment(author, event2, CommentState.PUBLISHED);
        createComment(author2, event2, CommentState.PENDING);

        entityManager.flush();
        entityManager.clear();

        List<Comment> result = commentRepository.findCommentsByEventIdsAndStateWithAuthorFetch(
                List.of(event1.getId(), event2.getId()), CommentState.PUBLISHED);

        assertThat(result).hasSize(3);
        assertThat(result)
                .extracting(c -> c.getEvent().getId())
                .containsOnly(event1.getId(), event2.getId());
        assertThat(result)
                .allMatch(c -> c.getState() == CommentState.PUBLISHED);
    }

    @Test
    void findCommentsByEventIdsAndStateWithAuthorFetchReturnsEmpty() {
        User author = createUser("1");
        Category category = createCategory();
        Event event = createEvent(author, category, "Event");

        entityManager.flush();
        entityManager.clear();

        List<Comment> result = commentRepository.findCommentsByEventIdsAndStateWithAuthorFetch(
                List.of(event.getId()), CommentState.PUBLISHED);

        assertThat(result).isEmpty();
    }

    @Test
    void findCommentsByEventIdsAndStateWithAuthorFetchIgnoresOtherEvents() {
        User author = createUser("1");
        User author2 = createUser("2");
        Category category = createCategory();
        Event event1 = createEvent(author, category, "Event 1");
        Event event2 = createEvent(author, category, "Event 2");

        createComment(author, event1, CommentState.PUBLISHED);
        createComment(author, event2, CommentState.PUBLISHED);
        createComment(author2, event2, CommentState.PUBLISHED);

        entityManager.flush();
        entityManager.clear();

        List<Comment> result = commentRepository.findCommentsByEventIdsAndStateWithAuthorFetch(
                List.of(event1.getId()), CommentState.PUBLISHED);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getEvent().getId()).isEqualTo(event1.getId());
    }

    private User createUser(String string) {
        User user = new User();
        user.setName(NAME + "-" + string);
        user.setEmail(user.getName() + "@mail.com");
        return entityManager.persistAndFlush(user);
    }

    private Category createCategory() {
        Category category = new Category();
        category.setName("Cat");
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

    private void createComment(User author, Event event, CommentState state) {
        Comment comment = Comment.builder()
                .author(author)
                .event(event)
                .text("Comment text")
                .state(state)
                .createdOn(LocalDateTime.now())
                .build();
        entityManager.persistAndFlush(comment);
    }

    private EventCommentsCount findById(List<EventCommentsCount> rows, Long eventId) {
        return rows.stream()
                .filter(r -> r.eventId().equals(eventId))
                .findFirst()
                .orElseThrow();
    }
}