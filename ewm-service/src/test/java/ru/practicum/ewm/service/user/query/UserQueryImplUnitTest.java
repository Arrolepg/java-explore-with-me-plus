package ru.practicum.ewm.service.user.query;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.service.exception.NotFoundException;
import ru.practicum.ewm.service.user.model.User;
import ru.practicum.ewm.service.user.repository.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserQueryImplUnitTest {
    private static final Long USER_ID = 1L;
    private static final String NAME = "User";
    private static final String EMAIL = "user@mail.com";

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserQueryImpl userQuery;

    @Test
    void testFindUser() {
        User user = createUser(USER_ID, NAME, EMAIL);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        User result = userQuery.findUser(USER_ID);

        assertThat(result).isEqualTo(user);
        assertThat(result.getId()).isEqualTo(USER_ID);
        assertThat(result.getName()).isEqualTo(NAME);
        assertThat(result.getEmail()).isEqualTo(EMAIL);

        verify(userRepository, times(1)).findById(USER_ID);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void testFindUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userQuery.findUser(99L))
                .isInstanceOf(NotFoundException.class);

        verify(userRepository, times(1)).findById(99L);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void testCheckUserExists() {
        when(userRepository.existsById(USER_ID)).thenReturn(true);

        userQuery.checkUserExists(USER_ID);

        verify(userRepository, times(1)).existsById(USER_ID);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void testCheckUserExistsNotFound() {
        when(userRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> userQuery.checkUserExists(99L))
                .isInstanceOf(NotFoundException.class);

        verify(userRepository, times(1)).existsById(99L);
        verifyNoMoreInteractions(userRepository);
    }

    private User createUser(Long id, String name, String email) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setEmail(email);
        return user;
    }
}