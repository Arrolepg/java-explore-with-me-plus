package ru.practicum.ewm.service.request.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<List<ParticipationRequestDto>> findAllByRequester(@PathVariable Long userId) {
        return ResponseEntity.ok(requestService.findAllByRequester(userId));
    }

    @PostMapping
    public ResponseEntity<ParticipationRequestDto> createRequest(@PathVariable Long userId,
                                                                 @RequestParam Long eventId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(requestService.create(userId, eventId));
    }

    @PatchMapping("/{requestId}/cancel")
    public ResponseEntity<ParticipationRequestDto> cancelRequest(@PathVariable Long userId,
                                                                 @PathVariable Long requestId) {
        return ResponseEntity.ok(requestService.cancelRequest(userId, requestId));
    }
}
