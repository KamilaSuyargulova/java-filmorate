package ru.yandex.practicum.filmorate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.mappers.FilmRowMapper;
import ru.yandex.practicum.filmorate.storage.mappers.GenreRowMapper;
import ru.yandex.practicum.filmorate.storage.mappers.MpaRatingRowMapper;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
@AutoConfigureTestDatabase
@Import({FilmDbStorage.class, FilmRowMapper.class, GenreRowMapper.class, MpaRatingRowMapper.class})
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class FilmDbStorageTest {

    private final FilmDbStorage filmDbStorage;
    private Film testFilm;

    @BeforeEach
    void setUp() {
        testFilm = new Film();
        testFilm.setName("Тестовый фильм");
        testFilm.setDescription("Тестовое описание");
        testFilm.setReleaseDate(LocalDate.of(2000, 1, 1));
        testFilm.setDuration(120);

        MpaRating mpa = new MpaRating();
        mpa.setId(1L);
        mpa.setName("G");
        testFilm.setMpa(mpa);

        Set<Genre> genres = new HashSet<>();
        Genre genre1 = new Genre();
        genre1.setId(1L);
        genre1.setName("Комедия");
        genres.add(genre1);

        testFilm.setGenres(genres);
    }

    @Test
    void testCreateFilm() {
        Film createdFilm = filmDbStorage.create(testFilm);

        assertNotNull(createdFilm);
        assertNotNull(createdFilm.getId());
        assertEquals("Тестовый фильм", createdFilm.getName());
        assertEquals("Тестовое описание", createdFilm.getDescription());
        assertEquals(LocalDate.of(2000, 1, 1), createdFilm.getReleaseDate());
        assertEquals(120, createdFilm.getDuration());
        assertNotNull(createdFilm.getMpa());
        assertEquals(1L, createdFilm.getMpa().getId());

        Optional<Film> foundFilm = filmDbStorage.findById(createdFilm.getId());
        assertThat(foundFilm).isPresent();
        assertEquals(createdFilm.getId(), foundFilm.get().getId());
    }

    @Test
    void testFindAllFilms() {
        filmDbStorage.create(testFilm);

        Film film2 = new Film();
        film2.setName("Второй фильм");
        film2.setDescription("Описание второго фильма");
        film2.setReleaseDate(LocalDate.of(2001, 1, 1));
        film2.setDuration(130);

        MpaRating mpa2 = new MpaRating();
        mpa2.setId(2L); // PG
        film2.setMpa(mpa2);
        filmDbStorage.create(film2);

        List<Film> films = filmDbStorage.findAll();

        assertNotNull(films);
        assertEquals(2, films.size());
        assertTrue(films.stream().anyMatch(f -> f.getName().equals("Тестовый фильм")));
        assertTrue(films.stream().anyMatch(f -> f.getName().equals("Второй фильм")));
    }

    @Test
    void testFindFilmById() {
        Film createdFilm = filmDbStorage.create(testFilm);

        Optional<Film> foundFilm = filmDbStorage.findById(createdFilm.getId());

        assertThat(foundFilm).isPresent();
        assertEquals(createdFilm.getId(), foundFilm.get().getId());
        assertEquals("Тестовый фильм", foundFilm.get().getName());
        assertEquals("Тестовое описание", foundFilm.get().getDescription());
        assertEquals(LocalDate.of(2000, 1, 1), foundFilm.get().getReleaseDate());
        assertEquals(120, foundFilm.get().getDuration());
    }

    @Test
    void testFindNonExistentFilmById() {
        Optional<Film> foundFilm = filmDbStorage.findById(999L);

        assertThat(foundFilm).isEmpty();
    }

    @Test
    void testUpdateFilm() {
        Film createdFilm = filmDbStorage.create(testFilm);

        createdFilm.setName("Обновленное название");
        createdFilm.setDescription("Обновленное описание");
        createdFilm.setReleaseDate(LocalDate.of(2005, 1, 1));
        createdFilm.setDuration(150);

        MpaRating newMpa = new MpaRating();
        newMpa.setId(3L); // PG-13
        createdFilm.setMpa(newMpa);

        Set<Genre> newGenres = new HashSet<>();
        Genre genre2 = new Genre();
        genre2.setId(2L); // Драма
        genre2.setName("Драма");
        newGenres.add(genre2);
        createdFilm.setGenres(newGenres);

        Film updatedFilm = filmDbStorage.update(createdFilm);

        assertEquals("Обновленное название", updatedFilm.getName());
        assertEquals("Обновленное описание", updatedFilm.getDescription());
        assertEquals(LocalDate.of(2005, 1, 1), updatedFilm.getReleaseDate());
        assertEquals(150, updatedFilm.getDuration());
        assertEquals(3L, updatedFilm.getMpa().getId());

        Optional<Film> foundFilm = filmDbStorage.findById(createdFilm.getId());
        assertThat(foundFilm).isPresent();
        assertEquals("Обновленное название", foundFilm.get().getName());
    }

    @Test
    void testExistsById() {
        Film createdFilm = filmDbStorage.create(testFilm);

        assertTrue(filmDbStorage.existsById(createdFilm.getId()));
        assertFalse(filmDbStorage.existsById(999L));
    }

    @Test
    void testGetPopularFilms() {
        for (int i = 1; i <= 5; i++) {
            Film film = new Film();
            film.setName("Фильм " + i);
            film.setDescription("Описание " + i);
            film.setReleaseDate(LocalDate.of(2000 + i, 1, 1));
            film.setDuration(100 + i * 10);

            MpaRating mpa = new MpaRating();
            mpa.setId(1L);
            film.setMpa(mpa);

            filmDbStorage.create(film);
        }

        List<Film> popularFilms = filmDbStorage.getPopularFilms(3);

        assertNotNull(popularFilms);
        assertEquals(3, popularFilms.size());
    }

    @Test
    void testFilmWithGenres() {
        Set<Genre> genres = new HashSet<>();

        Genre genre1 = new Genre();
        genre1.setId(1L);
        genre1.setName("Комедия");

        Genre genre2 = new Genre();
        genre2.setId(2L);
        genre2.setName("Драма");

        genres.add(genre1);
        genres.add(genre2);
        testFilm.setGenres(genres);

        Film createdFilm = filmDbStorage.create(testFilm);

        Optional<Film> foundFilm = filmDbStorage.findById(createdFilm.getId());
        assertThat(foundFilm).isPresent();
        assertNotNull(foundFilm.get().getGenres());

        List<Genre> genreList = List.copyOf(foundFilm.get().getGenres());
        assertEquals(2, genreList.size());

        assertEquals(1L, genreList.get(0).getId());
        assertEquals(2L, genreList.get(1).getId());
    }
}