package ru.yandex.practicum.filmorate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.genre.GenreDbStorage;
import ru.yandex.practicum.filmorate.storage.mappers.GenreRowMapper;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
@AutoConfigureTestDatabase
@Import({GenreDbStorage.class, GenreRowMapper.class})
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class GenreDbStorageTest {

    private final GenreDbStorage genreDbStorage;

    @Test
    void testFindAllGenres() {
        List<Genre> genres = genreDbStorage.findAll();

        assertNotNull(genres);
        assertEquals(6, genres.size());

        assertEquals(1L, genres.get(0).getId());
        assertEquals("Комедия", genres.get(0).getName());

        assertEquals(2L, genres.get(1).getId());
        assertEquals("Драма", genres.get(1).getName());

        assertEquals(6L, genres.get(5).getId());
        assertEquals("Боевик", genres.get(5).getName());
    }

    @Test
    void testFindGenreById() {
        Optional<Genre> genreOptional = genreDbStorage.findById(1L);

        assertThat(genreOptional).isPresent();
        assertEquals(1L, genreOptional.get().getId());
        assertEquals("Комедия", genreOptional.get().getName());

        Optional<Genre> dramaOptional = genreDbStorage.findById(2L);
        assertThat(dramaOptional).isPresent();
        assertEquals(2L, dramaOptional.get().getId());
        assertEquals("Драма", dramaOptional.get().getName());

        Optional<Genre> nonExistent = genreDbStorage.findById(999L);
        assertThat(nonExistent).isEmpty();
    }

    @Test
    void testExistsById() {
        assertTrue(genreDbStorage.existsById(1L));
        assertTrue(genreDbStorage.existsById(2L));
        assertTrue(genreDbStorage.existsById(3L));
        assertTrue(genreDbStorage.existsById(4L));
        assertTrue(genreDbStorage.existsById(5L));
        assertTrue(genreDbStorage.existsById(6L));
        assertFalse(genreDbStorage.existsById(999L));
    }
}