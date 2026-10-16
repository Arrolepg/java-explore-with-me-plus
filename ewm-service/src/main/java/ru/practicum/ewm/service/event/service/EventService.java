package ru.practicum.ewm.service.event.service;

import ru.practicum.ewm.service.event.utility.EventPaginationParams;
import ru.practicum.ewm.service.event.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.service.event.dto.request.NewEventDto;
import ru.practicum.ewm.service.event.dto.request.UpdateEventAdminRequest;
import ru.practicum.ewm.service.event.dto.request.UpdateEventUserRequest;
import ru.practicum.ewm.service.event.dto.response.EventFullDto;
import ru.practicum.ewm.service.event.dto.response.EventRequestStatusUpdateResult;
import ru.practicum.ewm.service.event.dto.response.EventShortDto;
import ru.practicum.ewm.service.event.utility.*;
import ru.practicum.ewm.service.request.dto.ParticipationRequestDto;

import java.util.List;

public interface EventService {
    EventFullDto create(Long userId, NewEventDto eventDto);

    EventFullDto update(ResourceReference resourceReference, UpdateEventUserRequest eventDto);

    List<ParticipationRequestDto> findRequests(ResourceReference resourceReference);

    EventRequestStatusUpdateResult updateRequests(ResourceReference resourceReference,
                                                  EventRequestStatusUpdateRequest statusUpdateRequestDto);

    List<EventShortDto> findAllByInitiator(Long userId, EventPaginationParams eventPaginationParams);

    EventFullDto findById(ResourceReference resourceReference);

    List<EventFullDto> findAllByAdmin(AdminEventSearchFilter searchFilter, EventPaginationParams eventPaginationParams);

    EventFullDto updateByAdmin(Long eventId, UpdateEventAdminRequest eventDto);

    List<EventShortDto> findAllPublic(PublicEventSearchFilter searchFilter, EventPaginationParams eventPaginationParams,
                                      EventStatsHitParams hitParams);

    EventFullDto findByIdPublic(Long eventId, EventStatsHitParams hitParams);
}
