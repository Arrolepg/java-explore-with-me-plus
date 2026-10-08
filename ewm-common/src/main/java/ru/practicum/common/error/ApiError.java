package ru.practicum.common.error;

import com.fasterxml.jackson.annotation.JsonFormat;
import ru.practicum.common.util.PatternDataTime;

import java.time.LocalDateTime;

public record ApiError(
    String status,
    String reason,
    String message,

    @JsonFormat(pattern = PatternDataTime.PATTERN)
    LocalDateTime timestamp
) {}