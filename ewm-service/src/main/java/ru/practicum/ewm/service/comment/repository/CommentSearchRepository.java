package ru.practicum.ewm.service.comment.repository;

import org.springframework.data.jpa.domain.Specification;
import ru.practicum.ewm.service.comment.model.Comment;
import ru.practicum.ewm.service.comment.utility.CommentPaginationParams;

import java.util.List;

public interface CommentSearchRepository {
    /**
     * Поиск событий по спецификации с подгрузкой категории и инициатора одним запросом.
     * Возвращает список без подсчёта общего количества (в отличие от Page).
     *
     * @param specification условия поиска
     * @param params      смещение, размер выборки
     */
    List<Comment> findAllWithFetch(Specification<Comment> specification, CommentPaginationParams params);

}
