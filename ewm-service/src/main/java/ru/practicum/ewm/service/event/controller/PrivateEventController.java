package ru.practicum.ewm.service.event.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.service.event.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.service.event.dto.request.NewEventDto;
import ru.practicum.ewm.service.event.dto.request.UpdateEventUserRequest;
import ru.practicum.ewm.service.event.dto.response.EventFullDto;
import ru.practicum.ewm.service.event.dto.response.EventRequestStatusUpdateResult;
import ru.practicum.ewm.service.event.dto.response.EventShortDto;
import ru.practicum.ewm.service.event.service.EventService;
import ru.practicum.ewm.service.event.utility.EventSearchRequest;
import ru.practicum.ewm.service.event.utility.ResourceReference;
import ru.practicum.ewm.service.request.dto.ParticipationRequestDto;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/users/{userId}/events")
public class PrivateEventController {
    private final EventService eventService;

    @GetMapping
    public List<EventShortDto> findAll(@PathVariable Long userId,
                                       @RequestParam(required = false, defaultValue = "0") Integer from,
                                       @RequestParam(required = false, defaultValue = "10") Integer size,
                                       @RequestParam(required = false, defaultValue = "id,asc") String sort) {
        log.info("Получен запрос от пользователя с id = {} на поиск всех своих созданных событий", userId);

        EventSearchRequest eventSearchRequest = new EventSearchRequest(from, size, sort);

        return eventService.findAll(userId, eventSearchRequest);
    }

    @GetMapping("/{eventId}")
    public EventFullDto findById(@PathVariable Long userId,
                                 @PathVariable Long eventId) {
        log.info("Получен запрос от пользователя с id = {} на поиск события с id = {}", userId, eventId);

        ResourceReference resourceReference = new ResourceReference(userId, eventId);

        return eventService.findById(resourceReference);
    }

    @GetMapping("/{eventId}/requests")
    public List<ParticipationRequestDto> findRequests(@PathVariable Long userId,
                                                     @PathVariable Long eventId) {
        log.info("Получен запрос от пользователя с id = {} на список запросы участия в событии с id = {}",
                userId, eventId);

        ResourceReference resourceReference = new ResourceReference(userId, eventId);

        return eventService.findRequests(resourceReference);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventFullDto create(@PathVariable Long userId,
                               @Valid @RequestBody NewEventDto eventDto) {
        log.info("Получен запрос от пользователя id = {} на создание события", userId);

        return eventService.create(userId, eventDto);
    }

    @PatchMapping("/{eventId}")
    public EventFullDto update(@PathVariable Long userId,
                               @PathVariable Long eventId,
                               @Valid @RequestBody UpdateEventUserRequest eventDto) {
        log.info("Получен запрос от пользователя с id = {} на редактирование события с id = {}", userId, eventId);

        ResourceReference resourceReference = new ResourceReference(userId, eventId);

        return eventService.update(resourceReference, eventDto);
    }

    @PatchMapping("/{eventId}/requests")
    public EventRequestStatusUpdateResult updateRequests(@PathVariable Long userId,
                                                         @PathVariable Long eventId,
                                                         @Valid @RequestBody
                                                             EventRequestStatusUpdateRequest statusUpdateRequestDto) {
        log.info("Получен запрос от пользователя с id = {} на обновление заявок на участие в событии с id {}",
                userId, eventId);

        ResourceReference resourceReference = new ResourceReference(userId, eventId);

        return eventService.updateRequests(resourceReference, statusUpdateRequestDto);
    }
}
