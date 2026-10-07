package ru.practicum.ewm.service.user.utility;

import lombok.experimental.UtilityClass;
import ru.practicum.ewm.service.user.dto.UserShortDto;
import ru.practicum.ewm.service.user.model.User;

@UtilityClass
public class UserMapper {
    public UserShortDto toUserShortDto(User user) {
        return UserShortDto.builder()
                .id(user.getId())
                .name(user.getName())
                .build();
    }
}
