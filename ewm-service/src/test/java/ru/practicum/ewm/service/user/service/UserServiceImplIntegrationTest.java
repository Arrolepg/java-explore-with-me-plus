package ru.practicum.ewm.service.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.service.exception.ConflictException;
import ru.practicum.ewm.service.exception.NotFoundException;
import ru.practicum.ewm.service.user.dto.NewUserRequest;
import ru.practicum.ewm.service.user.dto.UserDto;
import ru.practicum.ewm.service.user.repository.UserRepository;
import ru.practicum.ewm.service.user.utility.AdminUserSearchRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class UserServiceImplIntegrationTest {
    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    private UserDto first;
    private UserDto second;
    private UserDto third;

    @BeforeEach
    void setUp() {
        first = userService.create(new NewUserRequest("first@mail.com", "First"));
        second = userService.create(new NewUserRequest("second@mail.com", "Second"));
        third = userService.create(new NewUserRequest("third@mail.com", "Third"));
    }

    @Test
    void testFindAllWithoutIds() {
        List<UserDto> result = userService.findAll(new AdminUserSearchRequest(null, 0, 10));

        assertThat(result).extracting(UserDto::getId)
                .containsExactly(first.getId(), second.getId(), third.getId());
    }

    @Test
    void testFindAllWithIds() {
        List<UserDto> result = userService.findAll(
                new AdminUserSearchRequest(List.of(third.getId(), first.getId()), 0, 10));

        assertThat(result).extracting(UserDto::getId).containsExactly(first.getId(), third.getId());
    }

    @Test
    void testFindAllWithPagination() {
        List<UserDto> result = userService.findAll(new AdminUserSearchRequest(null, 2, 2));

        assertThat(result).extracting(UserDto::getId).containsExactly(third.getId());
    }

    @Test
    void testCreateWithDuplicateEmailThrowsConflict() {
        assertThatThrownBy(() -> userService.create(new NewUserRequest("first@mail.com", "Other")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void testDelete() {
        userService.delete(second.getId());

        assertThat(userRepository.existsById(second.getId())).isFalse();
    }

    @Test
    void testDeleteNotFound() {
        assertThatThrownBy(() -> userService.delete(999L))
                .isInstanceOf(NotFoundException.class);
    }
}
