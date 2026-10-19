package ru.practicum.ewm.service.event.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.query.QueryUtils;
import ru.practicum.ewm.service.event.model.Event;
import ru.practicum.ewm.service.event.model.Event_;

import java.util.List;

public class EventSearchRepositoryImpl implements EventSearchRepository {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Event> findAllWithFetch(Specification<Event> specification, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Event> query = cb.createQuery(Event.class);
        Root<Event> root = query.from(Event.class);
        root.fetch(Event_.category);
        root.fetch(Event_.initiator);

        Predicate predicate = specification.toPredicate(root, query, cb);
        if (predicate != null) {
            query.where(predicate);
        }
        query.orderBy(QueryUtils.toOrders(pageable.getSort(), root, cb));

        TypedQuery<Event> typedQuery = entityManager.createQuery(query);
        if (pageable.isPaged()) {
            typedQuery.setFirstResult((int) pageable.getOffset());
            typedQuery.setMaxResults(pageable.getPageSize());
        }

        return typedQuery.getResultList();
    }
}
