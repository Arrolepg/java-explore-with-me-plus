package ru.practicum.ewm.service.request.query;

import ru.practicum.ewm.service.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.service.request.model.Request;
import ru.practicum.ewm.service.request.model.RequestStatus;

import java.util.List;
import java.util.Map;

public interface RequestQuery {
    List<ParticipationRequestDto> findRequests(Long eventId);

    List<Request> findRequestsByIdsAndEventId(List<Long> requestsIds, Long eventId);

    long countRequestsByEventIdAndStatus(Long eventId, RequestStatus requestStatus);

    List<Request> findRequestsByEventIdAndStatus(Long eventId, RequestStatus requestStatus);

    Map<Long, Long> countRequestsByEventIdsAndStatus(List<Long> eventIds, RequestStatus requestStatus);
}
