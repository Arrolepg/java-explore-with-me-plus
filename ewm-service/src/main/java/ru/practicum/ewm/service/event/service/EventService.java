package ru.practicum.ewm.service.event.service;

import ru.practicum.ewm.service.event.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.service.event.dto.request.NewEventDto;
import ru.practicum.ewm.service.event.dto.request.UpdateEventUserRequest;
import ru.practicum.ewm.service.event.dto.response.EventFullDto;
import ru.practicum.ewm.service.event.dto.response.EventRequestStatusUpdateResult;
import ru.practicum.ewm.service.event.dto.response.EventShortDto;
import ru.practicum.ewm.service.event.utility.PrivateEventSearchRequest;
import ru.practicum.ewm.service.event.utility.ResourceReference;
import ru.practicum.ewm.service.request.dto.ParticipationRequestDto;

import java.util.List;

public interface EventService {
    EventFullDto create(Long userId, NewEventDto eventDto);

    EventFullDto update(ResourceReference resourceReference, UpdateEventUserRequest eventDto);

    List<ParticipationRequestDto> findRequests(ResourceReference resourceReference);

    EventRequestStatusUpdateResult updateRequests(ResourceReference resourceReference,
                                                  EventRequestStatusUpdateRequest statusUpdateRequestDto);

    List<EventShortDto> findAll(Long userId, PrivateEventSearchRequest privateEventSearchRequest);

    EventFullDto findById(ResourceReference resourceReference);
}
