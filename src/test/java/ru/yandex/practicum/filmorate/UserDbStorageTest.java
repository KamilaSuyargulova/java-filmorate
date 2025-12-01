package ru.yandex.practicum.filmorate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.mappers.UserRowMapper;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
@AutoConfigureTestDatabase
@Import({UserDbStorage.class, UserRowMapper.class})
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class UserDbStorageTest {

    private final UserDbStorage userDbStorage;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setEmail("test@example.com");
        testUser.setLogin("testuser");
        testUser.setName("Test User");
        testUser.setBirthday(LocalDate.of(1990, 1, 1));
    }

    @Test
    void testCreateUser() {
        User createdUser = userDbStorage.create(testUser);

        assertNotNull(createdUser);
        assertNotNull(createdUser.getId());
        assertEquals("test@example.com", createdUser.getEmail());
        assertEquals("testuser", createdUser.getLogin());
        assertEquals("Test User", createdUser.getName());
        assertEquals(LocalDate.of(1990, 1, 1), createdUser.getBirthday());

        Optional<User> foundUser = userDbStorage.findById(createdUser.getId());
        assertThat(foundUser).isPresent();
        assertEquals(createdUser.getId(), foundUser.get().getId());
    }

    @Test
    void testFindAllUsers() {
        userDbStorage.create(testUser);

        User user2 = new User();
        user2.setEmail("test2@example.com");
        user2.setLogin("testuser2");
        user2.setName("Test User 2");
        user2.setBirthday(LocalDate.of(1995, 1, 1));
        userDbStorage.create(user2);

        List<User> users = userDbStorage.findAll();

        assertNotNull(users);
        assertEquals(2, users.size());
    }

    @Test
    void testFindUserById() {
        User createdUser = userDbStorage.create(testUser);

        Optional<User> foundUser = userDbStorage.findById(createdUser.getId());

        assertThat(foundUser).isPresent();
        assertEquals(createdUser.getId(), foundUser.get().getId());
        assertEquals("test@example.com", foundUser.get().getEmail());
    }

    @Test
    void testUpdateUser() {
        User createdUser = userDbStorage.create(testUser);

        createdUser.setName("Updated Name");
        createdUser.setLogin("updatedlogin");

        User updatedUser = userDbStorage.update(createdUser);

        assertEquals("Updated Name", updatedUser.getName());
        assertEquals("updatedlogin", updatedUser.getLogin());

        Optional<User> foundUser = userDbStorage.findById(createdUser.getId());
        assertThat(foundUser).isPresent();
        assertEquals("Updated Name", foundUser.get().getName());
    }

    @Test
    void testExistsById() {
        User createdUser = userDbStorage.create(testUser);

        assertTrue(userDbStorage.existsById(createdUser.getId()));
        assertFalse(userDbStorage.existsById(999L));
    }
}