package ru.practicum.ewm.service.event.service;

import ru.practicum.ewm.service.event.dto.request.UpdateEventAdminRequest;
import ru.practicum.ewm.service.event.dto.response.EventFullDto;
import ru.practicum.ewm.service.event.utility.AdminEventSearchRequest;

import java.util.List;

public interface AdminEventService {
    List<EventFullDto> findAll(AdminEventSearchRequest searchRequest);

    EventFullDto update(Long eventId, UpdateEventAdminRequest eventDto);
}
