package ru.practicum.ewm.service.comment.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.service.comment.dto.request.CommentAuthorRequestDto;
import ru.practicum.ewm.service.comment.dto.response.CommentAuthorResponseDto;
import ru.practicum.ewm.service.comment.service.CommentService;
import ru.practicum.ewm.service.comment.utility.CommentPaginationParams;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/users/{userId}/comments")
public class PrivateCommentController {
    private final CommentService commentService;

    @GetMapping
    public List<CommentAuthorResponseDto> findAll(@PathVariable Long userId,
                                                  @Valid @ModelAttribute CommentPaginationParams params) {
        log.info("Получен запрос от пользователя с id = {} на поиск всех своих комментариев", userId);
        return commentService.findAllByAuthor(userId, params);
    }

    @GetMapping("/{commentId}")
    public CommentAuthorResponseDto findById(@PathVariable Long userId,
                                             @PathVariable Long commentId) {
        log.info("Получен запрос от пользователя с id = {} на поиск комментария с id = {}", userId, commentId);

        return commentService.findById(userId, commentId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentAuthorResponseDto create(@PathVariable Long userId,
                                           @Valid @RequestBody CommentAuthorRequestDto commentRequestDto,
                                           @RequestParam Long eventId) {
        log.info("Получен запрос от пользователя id = {} на добавление комментария к событию id = {}", userId, eventId);

        return commentService.create(userId, commentRequestDto, eventId);
    }

    @PatchMapping("/{commentId}")
    public CommentAuthorResponseDto update(@PathVariable Long userId,
                                           @PathVariable Long commentId,
                                           @Valid @RequestBody CommentAuthorRequestDto commentRequestDto) {
        log.info("Получен запрос от пользователя с id = {} на редактирование комментария с id = {}", userId, commentId);

        return commentService.update(userId, commentRequestDto, commentId);
    }

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long userId,
                       @PathVariable Long commentId) {
        log.info("Получен запрос от пользователя с id = {} на удаление комментария с id = {}", userId, commentId);

        commentService.delete(userId, commentId);
    }
}
