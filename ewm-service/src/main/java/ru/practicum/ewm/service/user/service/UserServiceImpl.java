package ru.practicum.ewm.service.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.service.exception.BadRequestException;
import ru.practicum.ewm.service.exception.ConflictException;
import ru.practicum.ewm.service.user.dto.NewUserRequest;
import ru.practicum.ewm.service.user.dto.UserDto;
import ru.practicum.ewm.service.user.model.User;
import ru.practicum.ewm.service.user.model.User_;
import ru.practicum.ewm.service.user.query.UserQuery;
import ru.practicum.ewm.service.user.repository.UserRepository;
import ru.practicum.ewm.service.user.utility.AdminUserSearchRequest;
import ru.practicum.ewm.service.user.utility.UserMapper;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {
    private static final Sort SORT_BY_ID_ASC = Sort.by(Sort.Direction.ASC, User_.ID);

    private final UserRepository userRepository;
    private final UserQuery userQuery;

    @Override
    public List<UserDto> findAll(AdminUserSearchRequest searchRequest) {
        validateSearchRequest(searchRequest);
        Pageable pageable = PageRequest.of(searchRequest.from() / searchRequest.size(), searchRequest.size(),
                SORT_BY_ID_ASC);

        List<User> users = searchRequest.ids() == null || searchRequest.ids().isEmpty()
                ? userRepository.findAllBy(pageable)
                : userRepository.findAllByIdIn(searchRequest.ids(), pageable);

        return users.stream()
                .map(UserMapper::toUserDto)
                .toList();
    }

    @Override
    @Transactional
    public UserDto create(NewUserRequest userRequest) {
        if (userRepository.existsByEmail(userRequest.getEmail())) {
            throw new ConflictException("Пользователь с email = " + userRequest.getEmail() + " уже существует");
        }

        User user = userRepository.save(UserMapper.toUser(userRequest));
        log.info("Создан пользователь с id = {}", user.getId());

        return UserMapper.toUserDto(user);
    }

    @Override
    @Transactional
    public void delete(Long userId) {
        userQuery.checkUserExists(userId);
        userRepository.deleteById(userId);
        log.info("Удалён пользователь с id = {}", userId);
    }

    private void validateSearchRequest(AdminUserSearchRequest searchRequest) {
        if (searchRequest.from() < 0) {
            throw new BadRequestException("Параметр from не может быть отрицательным");
        }
        if (searchRequest.size() <= 0) {
            throw new BadRequestException("Параметр size должен быть больше нуля");
        }
    }
}
