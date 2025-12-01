package ru.yandex.practicum.filmorate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.storage.mappers.MpaRatingRowMapper;
import ru.yandex.practicum.filmorate.storage.mpa.MpaRatingDbStorage;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
@AutoConfigureTestDatabase
@Import({MpaRatingDbStorage.class, MpaRatingRowMapper.class})
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class MpaRatingDbStorageTest {

    private final MpaRatingDbStorage mpaRatingDbStorage;

    @Test
    void testFindAllMpa() {
        List<MpaRating> mpaRatings = mpaRatingDbStorage.findAll();

        assertNotNull(mpaRatings);
        assertEquals(5, mpaRatings.size());

        assertTrue(mpaRatings.stream().anyMatch(mpa -> mpa.getName().equals("G")));
        assertTrue(mpaRatings.stream().anyMatch(mpa -> mpa.getName().equals("PG")));
        assertTrue(mpaRatings.stream().anyMatch(mpa -> mpa.getName().equals("PG-13")));
        assertTrue(mpaRatings.stream().anyMatch(mpa -> mpa.getName().equals("R")));
        assertTrue(mpaRatings.stream().anyMatch(mpa -> mpa.getName().equals("NC-17")));
    }

    @Test
    void testFindMpaById() {
        Optional<MpaRating> mpaOptional = mpaRatingDbStorage.findById(1L);

        assertThat(mpaOptional).isPresent();
        assertEquals(1L, mpaOptional.get().getId());
        assertEquals("G", mpaOptional.get().getName());
        assertNotNull(mpaOptional.get().getDescription());

        Optional<MpaRating> nonExistent = mpaRatingDbStorage.findById(999L);
        assertThat(nonExistent).isEmpty();
    }

    @Test
    void testExistsById() {
        assertTrue(mpaRatingDbStorage.existsById(1L));
        assertTrue(mpaRatingDbStorage.existsById(2L));
        assertTrue(mpaRatingDbStorage.existsById(3L));
        assertTrue(mpaRatingDbStorage.existsById(4L));
        assertTrue(mpaRatingDbStorage.existsById(5L));
        assertFalse(mpaRatingDbStorage.existsById(999L));
    }
}