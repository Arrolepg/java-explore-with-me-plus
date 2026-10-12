package ru.practicum.ewm.service.category.dto.response;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class CategoryResponseDto {
    private Long id;

    @NotBlank
    private String name;
}
