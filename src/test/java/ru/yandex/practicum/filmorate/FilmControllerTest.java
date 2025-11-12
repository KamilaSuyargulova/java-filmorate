package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.controller.FilmController;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.storage.film.InMemoryFilmStorage;
import ru.yandex.practicum.filmorate.storage.user.InMemoryUserStorage;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class FilmControllerTest {

    private FilmController filmController;
    private InMemoryUserStorage userStorage;

    @BeforeEach
    void setUp() {
        InMemoryFilmStorage filmStorage = new InMemoryFilmStorage();
        userStorage = new InMemoryUserStorage();
        FilmService filmService = new FilmService(filmStorage, userStorage);
        filmController = new FilmController(filmService);
    }

    @Test
    void createValidFilm() {
        Film film = createTestFilm();

        Film createdFilm = filmController.create(film);

        assertNotNull(createdFilm.getId());
        assertEquals("Фильм", createdFilm.getName());
        assertEquals("Описание", createdFilm.getDescription());
        assertEquals(120, createdFilm.getDuration());
    }

    @Test
    void createFilmWithEmptyName() {
        Film film = createTestFilm();
        film.setName("");

        ValidationException exception = assertThrows(ValidationException.class,
                () -> filmController.create(film));
        assertEquals("Название не может быть пустым", exception.getMessage());
    }

    @Test
    void createFilmWith200SymbolDescription() {
        Film film = createTestFilm();
        film.setDescription("A".repeat(200));

        Film createdFilm = filmController.create(film);
        assertNotNull(createdFilm.getId());
    }

    @Test
    void createFilmWith201SymbolDescription() {
        Film film = createTestFilm();
        film.setDescription("A".repeat(201));

        ValidationException exception = assertThrows(ValidationException.class,
                () -> filmController.create(film));
        assertEquals("Максимальная длина описания — 200 символов", exception.getMessage());
    }

    @Test
    void createFilmWithBlankDescription() {
        Film film = createTestFilm();
        film.setDescription("   ");

        Film createdFilm = filmController.create(film);
        assertNotNull(createdFilm.getId());
    }

    @Test
    void updateFilmWithPartialData() {
        Film film = createTestFilm();
        Film createdFilm = filmController.create(film);

        Film updateFilm = new Film();
        updateFilm.setId(createdFilm.getId());
        updateFilm.setName("Фильм2");
        updateFilm.setReleaseDate(LocalDate.of(2000, 1, 1));
        updateFilm.setDuration(120);

        Film updatedFilm = filmController.update(updateFilm);

        assertEquals("Фильм2", updatedFilm.getName());
        assertEquals("Описание", updatedFilm.getDescription());
        assertEquals(120, updatedFilm.getDuration());
    }

    @Test
    void createFilmWithExactCinemaBirthday() {
        Film film = createTestFilm();
        film.setReleaseDate(LocalDate.of(1895, 12, 28));

        Film createdFilm = filmController.create(film);
        assertNotNull(createdFilm.getId());
    }

    @Test
    void createFilmWithOneDayBeforeCinemaBirthday() {
        Film film = createTestFilm();
        film.setReleaseDate(LocalDate.of(1895, 12, 27));

        ValidationException exception = assertThrows(ValidationException.class,
                () -> filmController.create(film));
        assertEquals("Дата релиза не может быть раньше 28 декабря 1895 года", exception.getMessage());
    }

    @Test
    void createFilmWithNullReleaseDate() {
        Film film = createTestFilm();
        film.setReleaseDate(null);

        ValidationException exception = assertThrows(ValidationException.class,
                () -> filmController.create(film));
        assertEquals("Дата релиза обязательна", exception.getMessage());
    }

    @Test
    void createFilmWithZeroDuration() {
        Film film = createTestFilm();
        film.setDuration(0);

        ValidationException exception = assertThrows(ValidationException.class,
                () -> filmController.create(film));
        assertEquals("Продолжительность фильма должна быть положительным числом", exception.getMessage());
    }

    @Test
    void createFilmWithNegativeDuration() {
        Film film = createTestFilm();
        film.setDuration(-120);

        ValidationException exception = assertThrows(ValidationException.class,
                () -> filmController.create(film));
        assertEquals("Продолжительность фильма должна быть положительным числом", exception.getMessage());
    }

    @Test
    void findFilmById() {
        Film film = createTestFilm();
        Film createdFilm = filmController.create(film);

        Film foundFilm = filmController.findById(createdFilm.getId());

        assertNotNull(foundFilm);
        assertEquals(createdFilm.getId(), foundFilm.getId());
        assertEquals("Фильм", foundFilm.getName());
    }

    @Test
    void getAllFilms() {
        Film film1 = createTestFilm();
        Film film2 = createTestFilm();
        film2.setName("Фильм2");

        filmController.create(film1);
        filmController.create(film2);

        assertEquals(2, filmController.findAll().size());
    }

    @Test
    void addLikeWithNonExistentUser() {
        Film film = createTestFilm();
        Film createdFilm = filmController.create(film);

        ru.yandex.practicum.filmorate.exception.NotFoundException exception =
                assertThrows(ru.yandex.practicum.filmorate.exception.NotFoundException.class,
                        () -> filmController.addLike(createdFilm.getId(), 999L));
        assertEquals("Пользователь с id=999 не найден", exception.getMessage());
    }

    @Test
    void removeLikeWithNonExistentUser() {
        Film film = createTestFilm();
        Film createdFilm = filmController.create(film);

        ru.yandex.practicum.filmorate.exception.NotFoundException exception =
                assertThrows(ru.yandex.practicum.filmorate.exception.NotFoundException.class,
                        () -> filmController.removeLike(createdFilm.getId(), 999L));
        assertEquals("Пользователь с id=999 не найден", exception.getMessage());
    }

    @Test
    void addLikeWithNonExistentFilm() {
        ru.yandex.practicum.filmorate.exception.NotFoundException exception =
                assertThrows(ru.yandex.practicum.filmorate.exception.NotFoundException.class,
                        () -> filmController.addLike(999L, 1L));
        assertEquals("Фильм с id=999 не найден", exception.getMessage());
    }

    @Test
    void addAndRemoveLike() {
        // Сначала создаем пользователя
        ru.yandex.practicum.filmorate.model.User user = new ru.yandex.practicum.filmorate.model.User();
        user.setEmail("test@example.com");
        user.setLogin("testuser");
        user.setBirthday(LocalDate.of(1990, 1, 1));
        userStorage.create(user);

        Film film = createTestFilm();
        Film createdFilm = filmController.create(film);

        // Добавляем лайк
        filmController.addLike(createdFilm.getId(), user.getId());

        // Удаляем лайк
        filmController.removeLike(createdFilm.getId(), user.getId());
    }

    private Film createTestFilm() {
        Film film = new Film();
        film.setName("Фильм");
        film.setDescription("Описание");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);
        return film;
    }
}