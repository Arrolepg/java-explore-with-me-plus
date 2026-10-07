package ru.practicum.ewm.service.user.query;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.service.exception.NotFoundException;
import ru.practicum.ewm.service.user.model.User;
import ru.practicum.ewm.service.user.repository.UserRepository;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserQueryImpl implements UserQuery {
    private final UserRepository userRepository;

    @Override
    public User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(
                        () -> new NotFoundException("Пользователь с id = " + id + " не найден")
                );
    }

    @Override
    public void checkUserExists(Long id) {
        if (!userRepository.existsById(id)) {
            throw new NotFoundException("Пользователь с id = " + id + " не найден");
        }
    }
}
