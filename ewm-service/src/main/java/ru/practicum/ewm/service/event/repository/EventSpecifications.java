package ru.practicum.ewm.service.event.repository;

import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;
import ru.practicum.ewm.service.category.model.Category_;
import ru.practicum.ewm.service.event.model.Event;
import ru.practicum.ewm.service.event.model.EventState;
import ru.practicum.ewm.service.event.model.Event_;
import ru.practicum.ewm.service.event.utility.AdminEventSearchRequest;
import ru.practicum.ewm.service.user.model.User_;

import java.time.LocalDateTime;
import java.util.List;

@UtilityClass
public class EventSpecifications {
    public Specification<Event> forAdminSearch(AdminEventSearchRequest request) {
        return Specification.where(initiatorIn(request.users()))
                .and(stateIn(request.states()))
                .and(categoryIn(request.categories()))
                .and(eventDateFrom(request.rangeStart()))
                .and(eventDateTo(request.rangeEnd()));
    }

    private Specification<Event> initiatorIn(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return null;
        }
        return (root, query, cb) -> root.get(Event_.initiator).get(User_.id).in(userIds);
    }

    private Specification<Event> stateIn(List<EventState> states) {
        if (states == null || states.isEmpty()) {
            return null;
        }
        return (root, query, cb) -> root.get(Event_.state).in(states);
    }

    private Specification<Event> categoryIn(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return null;
        }
        return (root, query, cb) -> root.get(Event_.category).get(Category_.id).in(categoryIds);
    }

    private Specification<Event> eventDateFrom(LocalDateTime rangeStart) {
        if (rangeStart == null) {
            return null;
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get(Event_.eventDate), rangeStart);
    }

    private Specification<Event> eventDateTo(LocalDateTime rangeEnd) {
        if (rangeEnd == null) {
            return null;
        }
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get(Event_.eventDate), rangeEnd);
    }
}
