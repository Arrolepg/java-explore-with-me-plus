package ru.practicum.ewm.service.comment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommentAuthorRequestDto {
    @NotBlank(message = "Текст комментария должен быть указан")
    @Size(max = 2000, message = "Максимальный размер текста комментария - 2000 символов")
    private String text;
}
