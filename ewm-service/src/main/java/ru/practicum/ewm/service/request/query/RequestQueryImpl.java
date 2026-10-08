package ru.practicum.ewm.service.request.query;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.service.exception.NotFoundException;
import ru.practicum.ewm.service.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.service.request.model.Request;
import ru.practicum.ewm.service.request.model.RequestStatus;
import ru.practicum.ewm.service.request.repository.RequestRepository;
import ru.practicum.ewm.service.request.repository.projection.EventRequestsCount;
import ru.practicum.ewm.service.request.utility.RequestMapper;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
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
    public List<Request> findRequestsByIdsAndEventId(List<Long> requestsIds, Long eventId) {
        List<Request> requests = requestRepository.findRequestsByIdInAndEventId(requestsIds, eventId);
        checkRequests(requests, requestsIds);

        return requests;
    }

    @Override
    public long countRequestsByEventIdAndStatus(Long eventId, RequestStatus requestStatus) {
        return requestRepository.countRequestsByEventIdAndStatus(eventId, requestStatus);
    }

    @Override
    public List<Request> findRequestsByEventIdAndStatus(Long eventId, RequestStatus requestStatus) {
        return requestRepository.findRequestsByEventIdAndStatus(eventId, requestStatus);
    }

    @Override
    public Map<Long, Long> countRequestsByEventIdsAndStatus(List<Long> eventIds, RequestStatus requestStatus) {
        return requestRepository.countRequestsByEventIdsAndStatus(eventIds, requestStatus).stream()
                .collect(Collectors.toMap(
                        EventRequestsCount::eventId,
                        EventRequestsCount::count
                ));
    }

    private void checkRequests(List<Request> requests, List<Long> requestIds) {
        if (requests.size() != requestIds.size()) {
            Set<Long> foundIds = requests.stream()
                    .map(Request::getId)
                    .collect(Collectors.toSet());

            List<Long> missingIds = requestIds.stream()
                    .filter(requestId -> !foundIds.contains(requestId))
                    .toList();

            throw new NotFoundException("Заявки на участие с id = " + missingIds + " не найдены");
        }
    }
}
