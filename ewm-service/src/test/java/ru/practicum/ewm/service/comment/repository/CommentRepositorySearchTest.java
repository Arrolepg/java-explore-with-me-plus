package ru.practicum.ewm.service.comment.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.ewm.service.category.model.Category;
import ru.practicum.ewm.service.comment.model.Comment;
import ru.practicum.ewm.service.comment.model.CommentState;
import ru.practicum.ewm.service.config.JpaAuditingConfig;
import ru.practicum.ewm.service.event.model.Event;
import ru.practicum.ewm.service.event.model.EventLocation;
import ru.practicum.ewm.service.user.model.User;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
public class CommentRepositorySearchTest {
    private static final String NAME = "Author";

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CommentRepository commentRepository;

    @Test
    void findAllWithFetchAppliesSpecificationFilterByStateAndText() {
        // Создаем трех РАЗНЫХ авторов для соблюдения unique_comment (event_id, author_id)
        User author1 = createUser("1");
        User author2 = createUser("2");
        User author3 = createUser("3");
        Category category = createCategory();
        Event event = createEvent(author1, category);

        // У каждого комментария свой уникальный автор в рамках одного события
        createCommentWithText(author1, event, CommentState.PUBLISHED, "Matching text for search");
        createCommentWithText(author2, event, CommentState.PENDING, "Matching text for search");
        createCommentWithText(author3, event, CommentState.PUBLISHED, "Other random text");

        entityManager.flush();
        entityManager.clear();

        // Спецификация: фильтр по статусу PUBLISHED и подстроке в тексте
        Specification<Comment> spec = (root, query, cb) -> cb.and(
                cb.equal(root.get("state"), CommentState.PUBLISHED),
                cb.like(cb.lower(root.get("text")), "%matching%")
        );

        Pageable pageable =
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdOn"));
        List<Comment> result = commentRepository.findAllWithFetch(spec, pageable);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getText()).isEqualTo("Matching text for search");
        assertThat(result.getFirst().getState()).isEqualTo(CommentState.PUBLISHED);
        assertThat(result.getFirst().getAuthor().getName()).isEqualTo("Author-1");
    }

    @Test
    void findAllWithFetchSortsByCreateDateDescCorrectly() {
        User author1 = createUser("1");
        User author2 = createUser("2");
        Category category = createCategory();
        Event event = createEvent(author1, category);

        // Создаем комментарии от РАЗНЫХ авторов с искусственным сдвигом по времени
        Comment olderComment = Comment.builder()
                .author(author1)
                .event(event)
                .text("Older comment")
                .state(CommentState.PUBLISHED)
                .build();
        entityManager.persist(olderComment);

        Comment newerComment = Comment.builder()
                .author(author2)
                .event(event)
                .text("Newer comment")
                .state(CommentState.PUBLISHED)
                .build();
        entityManager.persist(newerComment);

        entityManager.flush();
        entityManager.clear();

        updateCreatedOn(olderComment.getId(), LocalDateTime.now().minusDays(2));
        updateCreatedOn(newerComment.getId(), LocalDateTime.now().minusDays(1));

        Specification<Comment> spec = (root, query, cb) -> cb.conjunction(); // Выбираем все записи

        Pageable pageable = PageRequest.of(
                0,
                10,
                Sort.by(Sort.Direction.DESC, "createdOn")
        );

        List<Comment> result = commentRepository.findAllWithFetch(spec, pageable);

        assertThat(result).hasSize(2);
        // Первым должен вернуться более новый комментарий (DESC)
        assertThat(result.get(0).getText()).isEqualTo("Newer comment");
        assertThat(result.get(1).getText()).isEqualTo("Older comment");
    }

    @Test
    void findAllWithFetchAppliesPaginationFromAndSizeCorrectly() {
        User author1 = createUser("1");
        User author2 = createUser("2");
        User author3 = createUser("3");
        Category category = createCategory();
        Event event = createEvent(author1, category);

        // Три комментария от трех разных авторов для пагинации
        createCommentWithText(author1, event, CommentState.PUBLISHED, "Comment 1");
        createCommentWithText(author2, event, CommentState.PUBLISHED, "Comment 2");
        createCommentWithText(author3, event, CommentState.PUBLISHED, "Comment 3");

        entityManager.flush();
        entityManager.clear();

        Specification<Comment> spec = (root, query, cb) -> cb.conjunction();

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(1, 1);

        List<Comment> result = commentRepository.findAllWithFetch(spec, pageable);

        // Должен вернуться ровно один комментарий, идущий вторым по порядку (Comment 2)
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getText()).isEqualTo("Comment 2");
    }

    // Вспомогательные методы генерации данных
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

    private Event createEvent(User initiator, Category category) {
        Event event = Event.builder()
                .annotation("Some annotation text here for event")
                .category(category)
                .description("Some description text here for event")
                .eventDate(LocalDateTime.now().plusDays(10))
                .eventLocation(EventLocation.builder().lat(55.75f).lon(37.62f).build())
                .paid(false)
                .participantLimit(10)
                .requestModeration(true)
                .title("Event")
                .initiator(initiator)
                .build();
        return entityManager.persistAndFlush(event);
    }

    private void createCommentWithText(User author, Event event, CommentState state, String text) {
        Comment comment = Comment.builder()
                .author(author)
                .event(event)
                .text(text)
                .state(state)
                .build();
        entityManager.persistAndFlush(comment);
    }

    private void updateCreatedOn(Long commentId, LocalDateTime dateTime) {
        entityManager.getEntityManager()
                .createQuery("update Comment as c set c.createdOn = :date where c.id = :id")
                .setParameter("date", dateTime)
                .setParameter("id", commentId)
                .executeUpdate();
    }
}