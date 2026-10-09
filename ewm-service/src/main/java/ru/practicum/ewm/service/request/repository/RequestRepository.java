package ru.practicum.ewm.service.request.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.practicum.ewm.service.request.model.Request;
import ru.practicum.ewm.service.request.model.RequestStatus;
import ru.practicum.ewm.service.request.repository.projection.EventRequestsCount;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface RequestRepository extends JpaRepository<Request, Long> {
    List<Request> findByRequester_Id(Long userId);

    Boolean existsByRequester_IdAndEvent_Id(Long userId, Long eventId);

    Optional<Request> findByIdAndRequester_Id(Long requestId, Long userId);

    List<Request> findRequestsByEventId(Long id);

    List<Request> findRequestsByIdInAndEventId(Collection<Long> ids, Long eventId);

    long countRequestsByEventIdAndStatus(Long eventId, RequestStatus status);

    List<Request> findRequestsByEventIdAndStatus(Long eventId, RequestStatus status);

    @Query("select " +
            "new ru.practicum.ewm.service.request.repository.projection.EventRequestsCount(r.event.id, count(r)) " +
            "from Request as r " +
            "where r.event.id in ?1 and r.status = ?2 " +
            "group by r.event.id ")
    List<EventRequestsCount> countRequestsByEventIdsAndStatus(List<Long> eventIds, RequestStatus status);
}
