package ru.yandex.practicum.filmorate.storage.mpa;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.storage.mappers.MpaRatingRowMapper;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MpaRatingDbStorage implements MpaRatingStorage {
    private final JdbcTemplate jdbcTemplate;
    private final MpaRatingRowMapper mpaRatingRowMapper;

    private static final String FIND_ALL_SQL = "SELECT * FROM mpa_ratings ORDER BY id";
    private static final String FIND_BY_ID_SQL = "SELECT * FROM mpa_ratings WHERE id = ?";
    private static final String EXISTS_BY_ID_SQL = "SELECT COUNT(*) FROM mpa_ratings WHERE id = ?";

    @Override
    public List<MpaRating> findAll() {
        return jdbcTemplate.query(FIND_ALL_SQL, mpaRatingRowMapper);
    }

    @Override
    public Optional<MpaRating> findById(Long id) {
        List<MpaRating> result = jdbcTemplate.query(FIND_BY_ID_SQL, mpaRatingRowMapper, id);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    @Override
    public boolean existsById(Long id) {
        Integer count = jdbcTemplate.queryForObject(EXISTS_BY_ID_SQL, Integer.class, id);
        return count != null && count > 0;
    }
}