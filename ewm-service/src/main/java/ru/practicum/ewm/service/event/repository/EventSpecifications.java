package ru.practicum.ewm.service.event.repository;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;
import ru.practicum.ewm.service.category.model.Category_;
import ru.practicum.ewm.service.event.model.Event;
import ru.practicum.ewm.service.event.model.EventState;
import ru.practicum.ewm.service.event.model.Event_;
import ru.practicum.ewm.service.event.utility.AdminEventSearchFilter;
import ru.practicum.ewm.service.event.utility.PublicEventSearchFilter;
import ru.practicum.ewm.service.request.model.Request;
import ru.practicum.ewm.service.request.model.RequestStatus;
import ru.practicum.ewm.service.request.model.Request_;
import ru.practicum.ewm.service.user.model.User_;

import java.time.LocalDateTime;
import java.util.List;

@UtilityClass
public class EventSpecifications {
    public Specification<Event> forAdminSearch(AdminEventSearchFilter searchFilter) {
        return Specification.where(initiatorIn(searchFilter.users()))
                .and(stateIn(searchFilter.states()))
                .and(categoryIn(searchFilter.categories()))
                .and(eventDateFrom(searchFilter.rangeStart()))
                .and(eventDateTo(searchFilter.rangeEnd()));
    }

    public Specification<Event> forPublicSearch(PublicEventSearchFilter searchFilter) {
        return Specification.where(onlyPublished())
                .and(textLike(searchFilter.text()))
                .and(categoryIn(searchFilter.categories()))
                .and(paidEqualsTo(searchFilter.paid()))
                .and(eventDateIn(searchFilter.rangeStart(), searchFilter.rangeEnd()))
                .and(onlyAvailable(searchFilter.onlyAvailable())
                );
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

    private Specification<Event> textLike(String text) {
        if (text == null) {
            return null;
        }

        String searchText = '%' + text.toLowerCase() + '%';
        return (root, query, criteriaBuilder) -> {
            Predicate annotationLike = criteriaBuilder.like(criteriaBuilder.lower(
                    root.get(Event_.ANNOTATION)),
                    searchText
            );
            Predicate descriptionLike = criteriaBuilder.like(criteriaBuilder.lower(
                    root.get(Event_.DESCRIPTION)),
                    searchText
            );

            return criteriaBuilder.or(annotationLike, descriptionLike);
        };
    }

    private Specification<Event> paidEqualsTo(Boolean paid) {
        if (paid == null) {
            return null;
        }

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get(Event_.PAID), paid);
    }

    private Specification<Event> eventDateGreaterThan(LocalDateTime rangeStart) {
        if (rangeStart == null) {
            return null;
        }
        return (root, query, cb) -> cb.greaterThan(root.get(Event_.EVENT_DATE), rangeStart);
    }

    private Specification<Event> eventDateLessThan(LocalDateTime rangeEnd) {
        if (rangeEnd == null) {
            return null;
        }
        return (root, query, cb) -> cb.lessThan(root.get(Event_.EVENT_DATE), rangeEnd);
    }

    private Specification<Event> eventDateIn(LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        if (rangeStart == null && rangeEnd == null) {
            return (root, query, criteriaBuilder) ->
                    criteriaBuilder.greaterThan(root.get(Event_.EVENT_DATE), LocalDateTime.now());
        }

        return Specification.where(eventDateGreaterThan(rangeStart))
                .and(eventDateLessThan(rangeEnd));
    }

    private Specification<Event> onlyAvailable(Boolean onlyAvailable) {
        if (!onlyAvailable) {
            return null;
        }

        return (root, query, criteriaBuilder) -> {
            Subquery<Long> confirmedCount = query.subquery(Long.class);
            Root<Request> requestRoot = confirmedCount.from(Request.class);

            confirmedCount.select(criteriaBuilder.count(requestRoot.get(Request_.ID)))
                    .where(criteriaBuilder.equal(requestRoot.get(Request_.EVENT).get(Event_.ID), root.get(Event_.ID)),
                            criteriaBuilder.equal(requestRoot.get(Request_.STATUS), RequestStatus.CONFIRMED)
                    );
            return criteriaBuilder.greaterThan(root.get(Event_.PARTICIPANT_LIMIT), confirmedCount);
        };
    }

    private Specification<Event> onlyPublished() {
        return (root, query, criteriaBuilder) -> {
            Predicate publishedOn = criteriaBuilder.isNotNull(root.get(Event_.PUBLISHED_ON));
            Predicate state = criteriaBuilder.equal(root.get(Event_.STATE), EventState.PUBLISHED);

            return criteriaBuilder.and(publishedOn, state);
        };
    }
}
