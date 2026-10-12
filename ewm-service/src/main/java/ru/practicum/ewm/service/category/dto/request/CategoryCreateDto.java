package ru.practicum.ewm.service.category.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class CategoryCreateDto {
    private Long id;

    @NotBlank
    private String name;
}
