package ru.practicum.ewm.service.comment.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import ru.practicum.ewm.service.comment.model.Comment;
import ru.practicum.ewm.service.comment.model.CommentSearchSort;
import ru.practicum.ewm.service.comment.utility.CommentPaginationParams;

import java.util.List;

public class CommentSearchRepositoryImpl implements CommentSearchRepository {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
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
