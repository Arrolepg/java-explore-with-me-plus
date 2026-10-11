package ru.practicum.ewm.service.user.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import ru.practicum.ewm.service.exception.BadRequestException;
import ru.practicum.ewm.service.exception.ConflictException;
import ru.practicum.ewm.service.exception.NotFoundException;
import ru.practicum.ewm.service.user.dto.NewUserRequest;
import ru.practicum.ewm.service.user.dto.UserDto;
import ru.practicum.ewm.service.user.model.User;
import ru.practicum.ewm.service.user.query.UserQuery;
import ru.practicum.ewm.service.user.repository.UserRepository;
import ru.practicum.ewm.service.user.utility.AdminUserSearchRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplUnitTest {
    private static final Long USER_ID = 1L;
    private static final String EMAIL = "user@mail.com";
    private static final String NAME = "User";

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserQuery userQuery;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void testFindAllWithoutIds() {
        when(userRepository.findAllBy(any(Pageable.class))).thenReturn(List.of(createUser()));

        List<UserDto> result = userService.findAll(new AdminUserSearchRequest(null, 0, 10));

        assertThat(result).hasSize(1);
        assertAll(
                () -> assertThat(result.getFirst().getId()).isEqualTo(USER_ID),
                () -> assertThat(result.getFirst().getEmail()).isEqualTo(EMAIL),
                () -> assertThat(result.getFirst().getName()).isEqualTo(NAME)
        );
        verify(userRepository, never()).findAllByIdIn(anyList(), any(Pageable.class));
    }

    @Test
    void testFindAllWithIds() {
        when(userRepository.findAllByIdIn(eq(List.of(USER_ID)), any(Pageable.class)))
                .thenReturn(List.of(createUser()));

        List<UserDto> result = userService.findAll(new AdminUserSearchRequest(List.of(USER_ID), 0, 10));

        assertThat(result).extracting(UserDto::getId).containsExactly(USER_ID);
        verify(userRepository, never()).findAllBy(any(Pageable.class));
    }

    @Test
    void testFindAllWithNegativeFromThrowsBadRequest() {
        assertThatThrownBy(() -> userService.findAll(new AdminUserSearchRequest(null, -1, 10)))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(userRepository);
    }

    @Test
    void testFindAllWithZeroSizeThrowsBadRequest() {
        assertThatThrownBy(() -> userService.findAll(new AdminUserSearchRequest(null, 0, 0)))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(userRepository);
    }

    @Test
    void testCreate() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(USER_ID);
            return user;
        });

        UserDto result = userService.create(new NewUserRequest(EMAIL, NAME));

        assertAll(
                () -> assertThat(result.getId()).isEqualTo(USER_ID),
                () -> assertThat(result.getEmail()).isEqualTo(EMAIL),
                () -> assertThat(result.getName()).isEqualTo(NAME)
        );
    }

    @Test
    void testCreateWithDuplicateEmailThrowsConflict() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> userService.create(new NewUserRequest(EMAIL, NAME)))
                .isInstanceOf(ConflictException.class);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testDelete() {
        userService.delete(USER_ID);

        verify(userQuery).checkUserExists(USER_ID);
        verify(userRepository).deleteById(USER_ID);
    }

    @Test
    void testDeleteNotFound() {
        doThrow(new NotFoundException("Пользователь с id = 1 не найден")).when(userQuery).checkUserExists(USER_ID);

        assertThatThrownBy(() -> userService.delete(USER_ID))
                .isInstanceOf(NotFoundException.class);
        verify(userRepository, never()).deleteById(any());
    }

    private User createUser() {
        User user = new User();
        user.setId(USER_ID);
        user.setEmail(EMAIL);
        user.setName(NAME);
        return user;
    }
}
