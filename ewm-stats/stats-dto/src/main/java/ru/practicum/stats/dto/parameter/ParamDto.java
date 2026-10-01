package ru.practicum.stats.dto.parameter;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.stats.dto.util.PatternDataTime;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParamDto {

    @NotNull(message = "Дата начала диапазона (start) обязательна для заполнения")
    @JsonFormat(pattern = PatternDataTime.PATTERN)
    private LocalDateTime start;

    @NotNull(message = "Дата конца диапазона (end) обязательна для заполнения")
    @JsonFormat(pattern = PatternDataTime.PATTERN)
    private LocalDateTime end;

    private List<String> uris;

    @Builder.Default
    private Boolean unique = false;
}
