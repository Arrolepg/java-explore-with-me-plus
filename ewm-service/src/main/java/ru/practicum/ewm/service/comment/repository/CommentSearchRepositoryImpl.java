package ru.practicum.ewm.service.comment.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.query.QueryUtils;
import ru.practicum.ewm.service.comment.model.Comment;
import ru.practicum.ewm.service.comment.model.CommentSearchSort;
import ru.practicum.ewm.service.comment.utility.CommentPaginationParams;
import jakarta.persistence.criteria.*;

import org.springframework.data.domain.Pageable;


import java.util.List;

public class CommentSearchRepositoryImpl implements CommentSearchRepository {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Comment> findAllWithFetch(Specification<Comment> specification, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Comment> query = cb.createQuery(Comment.class);
        Root<Comment> root = query.from(Comment.class);

        root.fetch("author", JoinType.LEFT);
        //root.fetch("event", JoinType.LEFT);

        Predicate predicate = specification.toPredicate(root, query, cb);
        if (predicate != null) {
            query.where(predicate);
        }
        query.orderBy(QueryUtils.toOrders(pageable.getSort(), root, cb));

        return entityManager.createQuery(query)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();
    }

    public List<Comment> findAllByAuthorId(Long userId, CommentPaginationParams params) {
        String jpql = "select c from Comment c where c.author.id = :userId";

        if (params.sort() == CommentSearchSort.CREATE_DATE) {
            jpql += " order by c.createdOn desc, c.id";
        } else {
            jpql += " order by c.id";
        }

        TypedQuery<Comment> query = entityManager.createQuery(jpql, Comment.class);
        query.setParameter("userId", userId);

        return query.setFirstResult(params.from()).setMaxResults(params.size()).getResultList();
    }
}
