package ru.practicum.ewm.service.request.service;

import ru.practicum.ewm.service.request.dto.ParticipationRequestDto;

import java.util.List;

public interface RequestService {
    ParticipationRequestDto create(Long userId, Long eventId);

    List<ParticipationRequestDto> findAllByRequester(Long userId);

    ParticipationRequestDto cancelRequest(Long userId, Long requestId);

}
