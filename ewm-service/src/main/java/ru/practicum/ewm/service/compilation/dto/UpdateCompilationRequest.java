package ru.practicum.ewm.service.compilation.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UpdateCompilationRequest {
    private List<Long> events;
    private Boolean pinned;

    @Size(min = 1, max = 50)
    private String title;
}
