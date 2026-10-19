package ru.practicum.ewm.service.user.query;

import ru.practicum.ewm.service.user.model.User;

public interface UserQuery {
    User findUser(Long id);

    void checkUserExists(Long id);
}
