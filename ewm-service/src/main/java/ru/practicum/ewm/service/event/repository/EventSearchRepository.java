package ru.practicum.ewm.service.event.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import ru.practicum.ewm.service.event.model.Event;

import java.util.List;

public interface EventSearchRepository {
    /**
     * Поиск событий по спецификации с подгрузкой категории и инициатора одним запросом.
     * Возвращает список без подсчёта общего количества (в отличие от Page).
     *
     * @param specification условия поиска
     * @param pageable      смещение, размер выборки и сортировка
     */
    List<Event> findAllWithFetch(Specification<Event> specification, Pageable pageable);
}
