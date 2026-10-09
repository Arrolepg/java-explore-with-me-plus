package ru.practicum.ewm.service.request.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.service.event.model.Event;
import ru.practicum.ewm.service.event.model.EventState;
import ru.practicum.ewm.service.event.query.EventQuery;
import ru.practicum.ewm.service.exception.ConflictException;
import ru.practicum.ewm.service.exception.NotFoundException;
import ru.practicum.ewm.service.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.service.request.model.Request;
import ru.practicum.ewm.service.request.model.RequestStatus;
import ru.practicum.ewm.service.request.repository.RequestRepository;
import ru.practicum.ewm.service.request.utility.RequestMapper;
import ru.practicum.ewm.service.user.model.User;
import ru.practicum.ewm.service.user.query.UserQuery;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {

    private final RequestRepository requestRepository;
    private final UserQuery userQuery;
    private final EventQuery eventQuery;

    @Override
    @Transactional
    public ParticipationRequestDto create(Long userId, Long eventId) {
        User user = userQuery.findUser(userId);
        Event event = eventQuery.findEvent(eventId);

        if (event.getInitiator().getId().equals(userId)) {
          throw new ConflictException("Инициатор события не может подавать запрос на участие в своём событии");
        }
        if (event.getState() != EventState.PUBLISHED) {
           throw new ConflictException("Событие не опубликовано");
         }
        if (requestRepository.existsByRequester_IdAndEvent_Id(userId, eventId)) {
            throw new ConflictException("Запрос был создан ранее");
        }

        long count = requestRepository.countRequestsByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        log.info("Количество подтвержденных заявок: count = {}", count);
        long limit = event.getParticipantLimit();
        log.info("Лимит заявок: limit = {}", limit);
        if ((limit > 0) && (limit <= count)) {
           throw new ConflictException("Достигнут лимит запросов на участие");
        }

        LocalDateTime timeCreate = LocalDateTime.now();
        Request request = new Request();
        request.setEvent(event);
        request.setCreated(timeCreate);
        request.setRequester(user);
        if (event.getRequestModeration()) {
            request.setStatus(RequestStatus.PENDING);
        } else {
            request.setStatus(RequestStatus.CONFIRMED);
        }

        requestRepository.save(request);
        log.info("Заявка пользователя сохранена: userId = {}, requestId = {}", userId, request.getId());
        return RequestMapper.toParticipationRequestDto(request);
    }

    @Override
    public List<ParticipationRequestDto> findAllByRequester(Long userId) {
        userQuery.findUser(userId);
        List<ParticipationRequestDto> requests = RequestMapper
                .toListParticipationRequestDto(requestRepository.findByRequester_Id(userId));
        log.info("Получены заявки пользователя: userId = {}, count = {}", userId, requests.size());
        return requests;
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        userQuery.findUser(userId);
        Request request = requestRepository
                .findByIdAndRequester_Id(requestId, userId)
                .orElseThrow(() -> new NotFoundException("Запрос на участие id = " + requestId
                        + " для пользователя id = " + userId + " не найден"));
        request.setStatus(RequestStatus.CANCELED);
        ParticipationRequestDto requestDto = RequestMapper.toParticipationRequestDto(requestRepository.save(request));
        log.info("Статус заявки пользователя изменен на 'CANCELED': userId = {}, requestId = {}", userId, requestId);
        return requestDto;
    }
}
