package ru.practicum.ewm.service.event.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.service.event.dto.response.EventShortDto;
import ru.practicum.ewm.service.event.service.EventService;
import ru.practicum.ewm.service.event.utility.EventStatsHitParams;
import ru.practicum.ewm.service.event.utility.PublicEventSearchFilter;
import ru.practicum.ewm.service.event.utility.PaginationParams;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/events")
public class PublicEventController {
    private static final String URI = "/events";

    private final EventService eventService;

    @GetMapping
    public List<EventShortDto> findAll(@Valid @ModelAttribute PublicEventSearchFilter searchFilter,
                                       @Valid @ModelAttribute PaginationParams paginationParams,
                                       HttpServletRequest httpServletRequest) {
        log.info("Получен запрос на поиск всех событий");
        log.info("searchFilter = {}, paginationParams = {}", searchFilter, paginationParams);

        EventStatsHitParams hitParams = new EventStatsHitParams(URI, httpServletRequest.getRemoteAddr());

        return eventService.findAllPublic(searchFilter, paginationParams, hitParams);
    }
}
