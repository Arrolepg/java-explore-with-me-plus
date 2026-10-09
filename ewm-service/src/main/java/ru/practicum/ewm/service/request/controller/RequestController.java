package ru.practicum.ewm.service.request.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.service.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.service.request.service.RequestService;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/users/{userId}/requests")
public class RequestController {

    private final RequestService requestService;

    @GetMapping
    public List<ParticipationRequestDto> findAllByRequester(@PathVariable Long userId) {
        return requestService.findAllByRequester(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipationRequestDto createRequest(@PathVariable Long userId,
                                                                 @RequestParam Long eventId) {
        return requestService.create(userId, eventId);
    }

    @PatchMapping("/{requestId}/cancel")
    public ParticipationRequestDto cancelRequest(@PathVariable Long userId,
                                                                 @PathVariable Long requestId) {
        return requestService.cancelRequest(userId, requestId);
    }
}
