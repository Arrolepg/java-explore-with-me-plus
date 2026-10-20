package ru.practicum.ewm.service.comment.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import ru.practicum.ewm.service.comment.model.AdminCommentStateAction;


@Data
public class CommentAdminUpdateDto {
    @NotNull(message = "Требуемый статус должен быть указан")
    private AdminCommentStateAction adminCommentStateAction;
}
