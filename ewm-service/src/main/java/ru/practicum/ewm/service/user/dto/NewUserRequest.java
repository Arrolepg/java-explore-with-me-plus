package ru.practicum.ewm.service.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewUserRequest {
    @NotBlank(message = "Почтовый адрес не может быть пустым")
    @Email(message = "Некорректный почтовый адрес")
    @Size(min = 6, max = 254, message = "Длина почтового адреса должна быть от 6 до 254 символов")
    private String email;

    @NotBlank(message = "Имя не может быть пустым")
    @Size(min = 2, max = 250, message = "Длина имени должна быть от 2 до 250 символов")
    private String name;
}
