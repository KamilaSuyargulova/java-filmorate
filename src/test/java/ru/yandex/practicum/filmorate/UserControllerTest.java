package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.controller.UserController;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class UserControllerTest {

    private UserController userController;

    @BeforeEach
    void setUp() {
        userController = new UserController();
    }

    @Test
    void createValidUser() {
        User user = createTestUser();

        User createdUser = userController.create(user);

        assertNotNull(createdUser.getId());
        assertEquals("test@example.com", createdUser.getEmail());
        assertEquals("testlogin", createdUser.getLogin());
        assertEquals("testlogin", createdUser.getName());
    }

    @Test
    void createUserWithName() {
        User user = createTestUser();
        user.setName("Имя");

        User createdUser = userController.create(user);

        assertEquals("Имя", createdUser.getName());
    }

    @Test
    void createUserWithEmptyEmail() {
        User user = createTestUser();
        user.setEmail("");

        ValidationException exception = assertThrows(ValidationException.class,
                () -> userController.create(user));
        assertEquals("Электронная почта не может быть пустой и должна содержать символ @",
                exception.getMessage());
    }

    @Test
    void createUserWithEmptyLogin() {
        User user = createTestUser();
        user.setLogin("");

        ValidationException exception = assertThrows(ValidationException.class,
                () -> userController.create(user));
        assertEquals("Логин не может быть пустым", exception.getMessage());
    }

    @Test
    void createUserWithLoginContainingSpaces() {
        User user = createTestUser();
        user.setLogin("test login");

        ValidationException exception = assertThrows(ValidationException.class,
                () -> userController.create(user));
        assertEquals("Логин не может содержать пробелы", exception.getMessage());
    }

    @Test
    void createUserWithFutureBirthday() {
        User user = createTestUser();
        user.setBirthday(LocalDate.now().plusDays(1));

        ValidationException exception = assertThrows(ValidationException.class,
                () -> userController.create(user));
        assertEquals("Дата рождения не может быть в будущем", exception.getMessage());
    }

    @Test
    void createUserWithEmptyName() {
        User user = createTestUser();
        user.setName("");

        User createdUser = userController.create(user);
        assertEquals("testlogin", createdUser.getName());
    }

    @Test
    void getAllUsers() {
        User user1 = createTestUser();
        User user2 = createTestUser();
        user2.setEmail("test2@example.com");
        user2.setLogin("testlogin2");

        userController.create(user1);
        userController.create(user2);

        assertEquals(2, userController.findAll().size());
    }

    private User createTestUser() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setLogin("testlogin");
        user.setBirthday(LocalDate.of(1990, 1, 1));
        return user;
    }
}