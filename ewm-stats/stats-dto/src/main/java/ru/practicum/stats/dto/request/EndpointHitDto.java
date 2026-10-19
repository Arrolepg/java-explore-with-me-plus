package ru.practicum.stats.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.common.util.PatternDataTime;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EndpointHitDto {
    private Long id;

    @NotBlank(message = "Идентификатор сервиса для которого записывается информация не может быть пустым")
    @Size(max = 1000, message = "Идентификатор сервиса не должен превышать 1000 символов")
    private String app;

    @NotBlank(message = "URI для которого был осуществлен запрос не может быть пустым")
    @Size(max = 1000, message = "URI для которого был осуществлен запрос не должен превышать 1000 символов")
    private String uri;

    @NotBlank(message = "IP-адрес пользователя, осуществившего запрос не может быть пустым")
    @Size(min = 7, max = 45, message = "IP-адрес должен быть длиной от 7 до 45 символов")
    private String ip;

    @NotNull(message = "Дата и время когда произошла ошибка не может быть пустой")
    @JsonFormat(pattern = PatternDataTime.PATTERN)
    private LocalDateTime timestamp;
}
