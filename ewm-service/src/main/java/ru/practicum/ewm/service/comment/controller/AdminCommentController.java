package ru.practicum.ewm.service.comment.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.service.comment.dto.request.CommentAdminUpdateDto;
import ru.practicum.ewm.service.comment.dto.response.CommentResponseDto;
import ru.practicum.ewm.service.comment.service.CommentService;
import ru.practicum.ewm.service.comment.utility.AdminCommentSearchFilter;
import ru.practicum.ewm.service.comment.utility.CommentPaginationParams;


import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/admin/comments")
public class AdminCommentController {
    private final CommentService commentService;

    @GetMapping
    public List<CommentResponseDto> findAll(
            @Valid @ModelAttribute AdminCommentSearchFilter searchFilter,
            @Valid @ModelAttribute CommentPaginationParams commentPaginationParams) {
        log.info("Получен запрос администратора на поиск комментариев: users = {}, events = {}, state = {}, "
                        + "from = {}, size = {}",
                searchFilter.users(), searchFilter.events(), searchFilter.state(),
                commentPaginationParams.from(), commentPaginationParams.size());

        return commentService.findAllByAdmin(searchFilter, commentPaginationParams);
    }

    @PatchMapping("/{commentId}")
    public CommentResponseDto update(@PathVariable Long commentId,
                                     @Valid @RequestBody CommentAdminUpdateDto commentDto) {
        log.info("Получен запрос администратора на принятие или отклонение комментария с id = {}", commentId);

        return commentService.updateByAdmin(commentId, commentDto);
    }
}
