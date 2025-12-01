package ru.yandex.practicum.filmorate.storage.genre;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.mappers.GenreRowMapper;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class GenreDbStorage implements GenreStorage {
    private final JdbcTemplate jdbcTemplate;
    private final GenreRowMapper genreRowMapper;

    private static final String FIND_ALL_SQL = "SELECT * FROM genres ORDER BY id";
    private static final String FIND_BY_ID_SQL = "SELECT * FROM genres WHERE id = ?";
    private static final String EXISTS_BY_ID_SQL = "SELECT COUNT(*) FROM genres WHERE id = ?";

    @Override
    public List<Genre> findAll() {
        return jdbcTemplate.query(FIND_ALL_SQL, genreRowMapper);
    }

    @Override
    public Optional<Genre> findById(Long id) {
        try {
            Genre genre = jdbcTemplate.queryForObject(FIND_BY_ID_SQL, genreRowMapper, id);
            return Optional.ofNullable(genre);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean existsById(Long id) {
        Integer count = jdbcTemplate.queryForObject(EXISTS_BY_ID_SQL, Integer.class, id);
        return count != null && count > 0;
    }
}