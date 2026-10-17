package ru.practicum.ewm.service.category.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryUpdateDto {
    @Size(max = 50, message = "Максимальная длина названия категории - 50 символов")
    private String name;
}
