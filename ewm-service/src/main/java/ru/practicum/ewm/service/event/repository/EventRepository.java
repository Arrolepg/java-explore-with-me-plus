package ru.practicum.ewm.service.event.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.practicum.ewm.service.event.model.Event;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    @Query("select e from Event as e " +
            "join fetch e.category " +
            "join fetch e.initiator " +
            "where e.id = ?1 ")
    Optional<Event> findByIdWithFetch(Long id);

    @Query("select e from Event as e " +
            "join fetch e.category " +
            "join fetch e.initiator " +
            "where e.initiator.id = ?1 ")
    List<Event> findEventsByInitiatorIdWithFetch(Long initiatorId, Pageable pageable);
}
