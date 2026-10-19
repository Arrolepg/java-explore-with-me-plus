package ru.practicum.ewm.service.comment.query;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.service.comment.repository.CommentRepository;

@Component
@RequiredArgsConstructor
public class CommentQueryImpl implements CommentQuery {
    private final CommentRepository commentRepository;
}
