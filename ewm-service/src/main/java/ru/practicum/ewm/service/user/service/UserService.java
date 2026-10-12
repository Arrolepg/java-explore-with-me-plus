package ru.practicum.ewm.service.user.service;

import ru.practicum.ewm.service.user.dto.NewUserRequest;
import ru.practicum.ewm.service.user.dto.UserDto;
import ru.practicum.ewm.service.user.utility.AdminUserSearchRequest;

import java.util.List;

public interface UserService {
    List<UserDto> findAll(AdminUserSearchRequest searchRequest);

    UserDto create(NewUserRequest userRequest);

    void delete(Long userId);
}
