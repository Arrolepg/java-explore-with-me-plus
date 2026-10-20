package ru.practicum.ewm.service.comment.repository;

import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;
import ru.practicum.ewm.service.comment.model.Comment;
import ru.practicum.ewm.service.comment.model.Comment_;
import ru.practicum.ewm.service.comment.utility.AdminCommentSearchFilter;
import ru.practicum.ewm.service.event.model.Event_;
import ru.practicum.ewm.service.user.model.User_;

import java.util.List;

@UtilityClass
public class CommentSpecifications {
    public Specification<Comment> forAdminSearch(AdminCommentSearchFilter searchFilter) {
        return Specification.where(authorIn(searchFilter.users()))
                .and(eventIn(searchFilter.events()))
                .and(stateEqualsTo(searchFilter.state()));
    }

    private Specification<Comment> authorIn(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return null;
        }
        return (root, query, cb) -> root.get(Comment_.AUTHOR).get(User_.ID).in(userIds);
    }

    private Specification<Comment> eventIn(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return null;
        }
        return (root, query, cb) -> root.get(Comment_.EVENT).get(Event_.ID).in(eventIds);
    }

    private Specification<Comment> stateEqualsTo(String state) {
        if (state == null) {
            return null;
        }

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get(Comment_.STATE), state);
    }

}
