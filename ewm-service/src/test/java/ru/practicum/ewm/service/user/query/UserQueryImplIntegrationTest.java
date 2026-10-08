package ru.practicum.ewm.service.user.query;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.service.exception.NotFoundException;
import ru.practicum.ewm.service.user.model.User;
import ru.practicum.ewm.service.user.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class UserQueryImplIntegrationTest {
    private static final String NAME = "User";
    private static final String EMAIL = "user@mail.com";

    @Autowired
    private UserQuery userQuery;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testFindUser() {
        User user = createUser(NAME, EMAIL);

        User result = userQuery.findUser(user.getId());

        assertThat(result.getId()).isEqualTo(user.getId());
        assertThat(result.getName()).isEqualTo(NAME);
        assertThat(result.getEmail()).isEqualTo(EMAIL);
    }

    @Test
    void testFindUserNotFound() {
        assertThatThrownBy(() -> userQuery.findUser(999L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void testCheckUserExists() {
        User user = createUser(NAME, EMAIL);

        userQuery.checkUserExists(user.getId());
    }

    @Test
    void testCheckUserExistsNotFound() {
        assertThatThrownBy(() -> userQuery.checkUserExists(999L))
                .isInstanceOf(NotFoundException.class);
    }

    private User createUser(String name, String email) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        return userRepository.save(user);
    }
}