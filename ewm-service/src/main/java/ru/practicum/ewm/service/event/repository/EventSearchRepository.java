package ru.practicum.ewm.service.event.repository;

import org.springframework.data.jpa.domain.Specification;
import ru.practicum.ewm.service.event.model.Event;

import java.util.List;

public interface EventSearchRepository {
    /**
     * Поиск событий по спецификации с подгрузкой категории и инициатора одним запросом.
     * Возвращает список без подсчёта общего количества (в отличие от Page).
     *
     * @param specification условия поиска
     * @param from          количество событий, которые нужно пропустить
     * @param size          количество событий в выборке
     */
    List<Event> findAllWithFetch(Specification<Event> specification, int from, int size);
}
