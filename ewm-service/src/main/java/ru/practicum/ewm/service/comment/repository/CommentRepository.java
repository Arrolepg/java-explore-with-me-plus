package ru.practicum.ewm.service.comment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.practicum.ewm.service.comment.model.Comment;
import ru.practicum.ewm.service.comment.model.CommentState;
import ru.practicum.ewm.service.comment.repository.projection.EventCommentsCount;

import java.util.List;

import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long>, CommentSearchRepository {
    @Query("select " +
            "new ru.practicum.ewm.service.comment.repository.projection.EventCommentsCount(c.event.id, count(c)) " +
            "from Comment as c " +
            "where c.event.id in ?1 and c.state = ?2 " +
            "group by c.event.id ")
    List<EventCommentsCount> countCommentsByEventIdsAndState(List<Long> eventIds, CommentState commentState);

    @Query("select c from Comment as c " +
            "join fetch c.author " +
            "where c.event.id = ?1 and c.state = ?2 ")
    List<Comment> findCommentsByEventIdAndStateWithAuthorFetch(Long eventId, CommentState commentState);

    @Query("select c from Comment as c " +
            "join fetch c.author " +
            "where c.event.id in ?1 and c.state = ?2 ")
    List<Comment> findCommentsByEventIdsAndStateWithAuthorFetch(List<Long> eventIds, CommentState commentState);

    Optional<Comment> findByIdAndAuthorId(Long commentId, Long userId);

    Boolean existsByAuthorIdAndEventId(Long userId, Long eventId);
}
}
