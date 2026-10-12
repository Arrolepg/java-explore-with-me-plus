package ru.practicum.ewm.service.category.dto.response;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class CategoryResponseDto {
    private Long id;

    @NotBlank
    @Size(max = 50, message = "Название категории должен быть длиной до 50 символов")
    private String name;
}
