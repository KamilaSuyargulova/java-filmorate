package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.controller.FilmController;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class FilmControllerTest {

    private FilmController filmController;

    @BeforeEach
    void setUp() {
        filmController = new FilmController();
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

    private Film createTestFilm() {
        Film film = new Film();
        film.setName("Фильм");
        film.setDescription("Описание");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);
        return film;
    }
}