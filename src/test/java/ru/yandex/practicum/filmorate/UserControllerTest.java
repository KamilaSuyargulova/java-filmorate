package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.controller.UserController;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.UserService;
import ru.yandex.practicum.filmorate.storage.user.InMemoryUserStorage;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class UserControllerTest {

    private UserController userController;

    @BeforeEach
    void setUp() {
        InMemoryUserStorage userStorage = new InMemoryUserStorage();
        UserService userService = new UserService(userStorage);
        userController = new UserController(userService);
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
    void updateUserWithPartialData() {
        User user = createTestUser();
        User createdUser = userController.create(user);

        User updateUser = new User();
        updateUser.setId(createdUser.getId());
        updateUser.setEmail("new@example.com");

        User updatedUser = userController.update(updateUser);

        assertEquals("new@example.com", updatedUser.getEmail());
        assertEquals("testlogin", updatedUser.getLogin());
        assertEquals("testlogin", updatedUser.getName());
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
    void createUserWithInvalidEmailFormat() {
        User user = createTestUser();
        user.setEmail("invalid-email-format");

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

    @Test
    void findUserById() {
        User user = createTestUser();
        User createdUser = userController.create(user);

        User foundUser = userController.findById(createdUser.getId());

        assertNotNull(foundUser);
        assertEquals(createdUser.getId(), foundUser.getId());
        assertEquals("test@example.com", foundUser.getEmail());
    }

    @Test
    void addAndGetFriends() {
        User user1 = createTestUser();
        User user2 = createTestUser();
        user2.setEmail("friend@example.com");
        user2.setLogin("friend");

        User createdUser1 = userController.create(user1);
        User createdUser2 = userController.create(user2);

        userController.addFriend(createdUser1.getId(), createdUser2.getId());

        assertEquals(1, userController.getFriends(createdUser1.getId()).size());
        assertEquals(createdUser2.getId(), userController.getFriends(createdUser1.getId()).get(0).getId());
    }

    @Test
    void getCommonFriends() {
        User user1 = createTestUser();
        User user2 = createTestUser();
        user2.setEmail("user2@example.com");
        user2.setLogin("user2");
        User commonFriend = createTestUser();
        commonFriend.setEmail("common@example.com");
        commonFriend.setLogin("common");

        User createdUser1 = userController.create(user1);
        User createdUser2 = userController.create(user2);
        User createdCommonFriend = userController.create(commonFriend);

        userController.addFriend(createdUser1.getId(), createdCommonFriend.getId());
        userController.addFriend(createdUser2.getId(), createdCommonFriend.getId());

        assertEquals(1, userController.getCommonFriends(createdUser1.getId(), createdUser2.getId()).size());
        assertEquals(createdCommonFriend.getId(),
                userController.getCommonFriends(createdUser1.getId(), createdUser2.getId()).get(0).getId());
    }

    private User createTestUser() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setLogin("testlogin");
        user.setBirthday(LocalDate.of(1990, 1, 1));
        return user;
    }
}