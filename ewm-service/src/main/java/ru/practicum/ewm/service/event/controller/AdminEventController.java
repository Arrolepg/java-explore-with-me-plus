package ru.practicum.ewm.service.event.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.service.event.dto.request.UpdateEventAdminRequest;
import ru.practicum.ewm.service.event.dto.response.EventFullDto;
import ru.practicum.ewm.service.event.service.EventService;
import ru.practicum.ewm.service.event.utility.AdminEventSearchFilter;
import ru.practicum.ewm.service.event.utility.PaginationParams;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/admin/events")
public class AdminEventController {
    private final EventService eventService;

    @GetMapping
    public List<EventFullDto> findAll(
            @Valid @ModelAttribute AdminEventSearchFilter searchFilter,
            @Valid @ModelAttribute PaginationParams paginationParams) {
        log.info("Получен запрос администратора на поиск событий: users = {}, states = {}, categories = {}, "
                + "rangeStart = {}, rangeEnd = {}, from = {}, size = {}",
                searchFilter.users(), searchFilter.states(), searchFilter.categories(), searchFilter.rangeStart(),
                searchFilter.rangeEnd(), paginationParams.from(), paginationParams.size());

        return eventService.findAllByAdmin(searchFilter, paginationParams);
    }

    @PatchMapping("/{eventId}")
    public EventFullDto update(@PathVariable Long eventId,
                               @Valid @RequestBody UpdateEventAdminRequest eventDto) {
        log.info("Получен запрос администратора на редактирование события с id = {}", eventId);

        return eventService.updateByAdmin(eventId, eventDto);
    }
}
