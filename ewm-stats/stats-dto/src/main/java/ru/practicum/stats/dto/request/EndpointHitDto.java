package ru.practicum.stats.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EndpointHitDto {
    private Long id;

    @NotNull(message = "Идентификатор сервиса для которого записывается информация не может быть пустым")
    @NotBlank(message = "Идентификатор сервиса для которого записывается информация не может быть пустым")
    private String app;


    @NotNull(message = "IP-адрес пользователя, осуществившего запрос не может быть пустым")
    @NotBlank(message = "IP-адрес пользователя, осуществившего запрос не может быть пустым")
    private String ip;

    @NotNull(message = "Дата и время когда произошла ошибка не может быть пустой")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
}
