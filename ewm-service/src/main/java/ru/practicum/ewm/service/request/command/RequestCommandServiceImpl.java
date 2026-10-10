package ru.practicum.ewm.service.request.command;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.service.exception.ConflictException;
import ru.practicum.ewm.service.exception.NotFoundException;
import ru.practicum.ewm.service.request.model.Request;
import ru.practicum.ewm.service.request.model.RequestStatus;
import ru.practicum.ewm.service.request.query.RequestQuery;
import ru.practicum.ewm.service.request.repository.RequestRepository;
import ru.practicum.ewm.service.request.utility.RequestMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class RequestCommandServiceImpl implements RequestCommandService {
    private final RequestRepository requestRepository;
    private final RequestQuery requestQuery;

    @Override
    public RequestStatusUpdateResult updateRequestsStatuses(RequestStatusUpdateCommand command) {
        List<Request> requests = requestRepository.findRequestsByIdInAndEventId(command.requestsIds(), command.eventId());
        checkRequestsSizeAndStatus(requests, command.requestsIds());

        long currentConfirmed = 0L;
        if (command.requestStatus() == RequestStatus.CONFIRMED) {
            currentConfirmed = countCurrentConfirmedAndCheckRequestLimit(command.eventId(), command.requestStatus(),
                    command.participantLimit());

        }

        List<Request> confirmedRequests = new ArrayList<>();
        List<Request> rejectedRequests = new ArrayList<>();
        processRequests(command, requests, currentConfirmed, confirmedRequests, rejectedRequests);

        return new RequestStatusUpdateResult(RequestMapper.toListParticipationRequestDto(confirmedRequests),
                RequestMapper.toListParticipationRequestDto(rejectedRequests));
    }

    private void checkRequestsSizeAndStatus(List<Request> requests, List<Long> requestIds) {
        if (requests.size() != requestIds.size()) {
            Set<Long> foundIds = requests.stream()
                    .map(Request::getId)
                    .collect(Collectors.toSet());

            List<Long> missingIds = requestIds.stream()
                    .filter(requestId -> !foundIds.contains(requestId))
                    .toList();

            throw new NotFoundException("Заявки на участие с id = " + missingIds + " не найдены");
        }

        if (requests.stream()
                .anyMatch(request -> request.getStatus() != RequestStatus.PENDING)
        ) {
            throw new ConflictException("Изменять статус можно только у заявки со статусом " + RequestStatus.PENDING);
        }
    }

    private long countCurrentConfirmedAndCheckRequestLimit(Long eventId, RequestStatus status, Integer limit) {
        long confirmedCount = requestQuery.countRequestsByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);

        if (status == RequestStatus.CONFIRMED && limit > 0 && confirmedCount >= limit) {
            throw new ConflictException("Достигнут лимит подтвержденный заявок на участие в событии с id = " + eventId);
        }

        return confirmedCount;
    }

    private void processRequests(RequestStatusUpdateCommand command,
                                 List<Request> requests,
                                 long currentConfirmed,
                                 List<Request> confirmedRequests,
                                 List<Request> rejectedRequests) {
        if (command.requestStatus() == RequestStatus.CONFIRMED) {
            for (Request request : requests) {
                if (command.participantLimit() > 0 && currentConfirmed >= command.participantLimit()) {
                    request.setStatus(RequestStatus.REJECTED);
                    rejectedRequests.add(request);
                } else {
                    request.setStatus(RequestStatus.CONFIRMED);
                    confirmedRequests.add(request);
                    currentConfirmed++;
                }
            }

            rejectRemainingRequests(command.participantLimit(), currentConfirmed, command.eventId(), rejectedRequests,
                    command.requestsIds());
        } else {
            requests.forEach(request -> {
                request.setStatus(RequestStatus.REJECTED);
                rejectedRequests.add(request);
            });
        }
    }

    private void rejectRemainingRequests(int participantLimit, long currentConfirmed, Long eventId,
                                         List<Request> rejectedRequests, List<Long> requestIds) {
        if (participantLimit > 0 && currentConfirmed >= participantLimit) {
            requestRepository.findRequestsByEventIdAndStatus(eventId, RequestStatus.PENDING).stream()
                    .filter(request -> !requestIds.contains(request.getId()))
                    .forEach(request -> {
                        request.setStatus(RequestStatus.REJECTED);
                        rejectedRequests.add(request);
                    });
        }
    }
}
