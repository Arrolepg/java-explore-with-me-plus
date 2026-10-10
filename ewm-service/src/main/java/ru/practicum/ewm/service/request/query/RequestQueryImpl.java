package ru.practicum.ewm.service.request.query;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.service.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.service.request.model.Request;
import ru.practicum.ewm.service.request.model.RequestStatus;
import ru.practicum.ewm.service.request.repository.RequestRepository;
import ru.practicum.ewm.service.request.repository.projection.EventRequestsCount;
import ru.practicum.ewm.service.request.utility.RequestMapper;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RequestQueryImpl implements RequestQuery {
    private final RequestRepository requestRepository;

    @Override
    public List<ParticipationRequestDto> findRequests(Long eventId) {
        List<Request> requests = requestRepository.findRequestsByEventId(eventId);
        if (requests.isEmpty()) {
            return List.of();
        }
        return RequestMapper.toListParticipationRequestDto(requests);
    }

    @Override
    public long countRequestsByEventIdAndStatus(Long eventId, RequestStatus requestStatus) {
        return requestRepository.countRequestsByEventIdAndStatus(eventId, requestStatus);
    }

    @Override
    public Map<Long, Long> countRequestsByEventIdsAndStatus(List<Long> eventIds, RequestStatus requestStatus) {
        return requestRepository.countRequestsByEventIdsAndStatus(eventIds, requestStatus).stream()
                .collect(Collectors.toMap(
                        EventRequestsCount::eventId,
                        EventRequestsCount::count
                ));
    }
}
