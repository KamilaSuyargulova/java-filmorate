package ru.yandex.practicum.filmorate.storage.film;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.storage.BaseRepository;
import ru.yandex.practicum.filmorate.storage.mappers.FilmRowMapper;
import ru.yandex.practicum.filmorate.storage.mappers.GenreRowMapper;
import ru.yandex.practicum.filmorate.storage.mappers.MpaRatingRowMapper;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

@Slf4j
@Repository
@Qualifier("filmDbStorage")
public class FilmDbStorage extends BaseRepository<Film> implements FilmStorage {
    private final JdbcTemplate jdbcTemplate;
    private final FilmRowMapper filmRowMapper;
    private final GenreRowMapper genreRowMapper;
    private final MpaRatingRowMapper mpaRatingRowMapper;

    private static final String FIND_ALL_SQL = """
            SELECT f.*, m.id as mpa_id, m.name as mpa_name, m.description as mpa_description
            FROM films f
            LEFT JOIN mpa_ratings m ON f.mpa_rating_id = m.id
            ORDER BY f.id
            """;
    private static final String FIND_BY_ID_SQL = """
            SELECT f.*, m.id as mpa_id, m.name as mpa_name, m.description as mpa_description
            FROM films f
            LEFT JOIN mpa_ratings m ON f.mpa_rating_id = m.id
            WHERE f.id = ?
            """;
    private static final String INSERT_SQL = "INSERT INTO films (name, description, release_date, duration, " +
            "mpa_rating_id) VALUES (?, ?, ?, ?, ?)";
    private static final String UPDATE_SQL = "UPDATE films SET name = ?, description = ?, release_date = ?, " +
            "duration = ?, mpa_rating_id = ? WHERE id = ?";
    private static final String DELETE_SQL = "DELETE FROM films WHERE id = ?";
    private static final String EXISTS_BY_ID_SQL = "SELECT COUNT(*) FROM films WHERE id = ?";
    private static final String ADD_LIKE_SQL = "INSERT INTO likes (film_id, user_id) VALUES (?, ?)";
    private static final String REMOVE_LIKE_SQL = "DELETE FROM likes WHERE film_id = ? AND user_id = ?";
    private static final String GET_LIKES_SQL = "SELECT user_id FROM likes WHERE film_id = ?";
    private static final String GET_POPULAR_SQL = """
            SELECT f.*, m.id as mpa_id, m.name as mpa_name, m.description as mpa_description,
                   COUNT(l.user_id) as likes_count
            FROM films f
            LEFT JOIN mpa_ratings m ON f.mpa_rating_id = m.id
            LEFT JOIN likes l ON f.id = l.film_id
            GROUP BY f.id, m.id, m.name, m.description
            ORDER BY COUNT(l.user_id) DESC
            LIMIT ?
            """;
    private static final String ADD_GENRE_SQL = "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)";
    private static final String REMOVE_GENRES_SQL = "DELETE FROM film_genres WHERE film_id = ?";
    private static final String GET_FILMS_GENRES_SQL = """
            SELECT fg.film_id, g.* FROM genres g
            JOIN film_genres fg ON g.id = fg.genre_id
            WHERE fg.film_id IN (%s)
            ORDER BY fg.film_id, g.id
            """;
    private static final String GET_FILMS_LIKES_SQL = """
            SELECT film_id, user_id FROM likes
            WHERE film_id IN (%s)
            ORDER BY film_id
            """;

    public FilmDbStorage(JdbcTemplate jdbcTemplate, FilmRowMapper filmRowMapper,
                         GenreRowMapper genreRowMapper, MpaRatingRowMapper mpaRatingRowMapper) {
        super(jdbcTemplate, filmRowMapper);
        this.jdbcTemplate = jdbcTemplate;
        this.filmRowMapper = filmRowMapper;
        this.genreRowMapper = genreRowMapper;
        this.mpaRatingRowMapper = mpaRatingRowMapper;
    }

    @Override
    public List<Film> findAll() {
        List<Film> films = jdbcTemplate.query(FIND_ALL_SQL, rs -> {
            List<Film> result = new ArrayList<>();
            Map<Long, Film> filmMap = new HashMap<>();

            while (rs.next()) {
                Long filmId = rs.getLong("id");
                Film film = filmMap.get(filmId);

                if (film == null) {
                    film = filmRowMapper.mapRow(rs, rs.getRow());

                    MpaRating mpa = new MpaRating();
                    mpa.setId(rs.getLong("mpa_id"));
                    mpa.setName(rs.getString("mpa_name"));
                    mpa.setDescription(rs.getString("mpa_description"));
                    film.setMpa(mpa);

                    filmMap.put(filmId, film);
                    result.add(film);
                }
            }

            if (!filmMap.isEmpty()) {
                loadFilmsData(filmMap);
            }

            return result;
        });

        return films;
    }

    @Override
    public Film create(Film film) {
        Long mpaId = film.getMpa().getId();
        long id = insert(INSERT_SQL,
                film.getName(),
                film.getDescription(),
                film.getReleaseDate(),
                film.getDuration(),
                mpaId
        );
        film.setId(id);
        saveGenres(film);
        return film;
    }

    @Override
    public Film update(Film film) {
        if (film.getMpa() == null) {
            MpaRating defaultMpa = new MpaRating();
            defaultMpa.setId(1L);
            film.setMpa(defaultMpa);
        }

        Long mpaId = film.getMpa().getId();

        update(UPDATE_SQL,
                film.getName(),
                film.getDescription(),
                film.getReleaseDate(),
                film.getDuration(),
                mpaId,
                film.getId()
        );

        updateGenres(film);
        return film;
    }

    @Override
    public Optional<Film> findById(Long id) {
        List<Film> films = jdbcTemplate.query(FIND_BY_ID_SQL, rs -> {
            List<Film> result = new ArrayList<>();
            Map<Long, Film> filmMap = new HashMap<>();

            while (rs.next()) {
                Long filmId = rs.getLong("id");
                Film film = filmMap.get(filmId);

                if (film == null) {
                    film = filmRowMapper.mapRow(rs, rs.getRow());

                    MpaRating mpa = new MpaRating();
                    mpa.setId(rs.getLong("mpa_id"));
                    mpa.setName(rs.getString("mpa_name"));
                    mpa.setDescription(rs.getString("mpa_description"));
                    film.setMpa(mpa);

                    filmMap.put(filmId, film);
                    result.add(film);
                }
            }

            if (!filmMap.isEmpty()) {
                loadFilmsData(filmMap);
            }

            return result;
        }, id);

        return films.isEmpty() ? Optional.empty() : Optional.of(films.get(0));
    }

    @Override
    public void delete(Long id) {
        delete(DELETE_SQL, id);
    }

    @Override
    public boolean existsById(Long id) {
        Integer count = jdbcTemplate.queryForObject(EXISTS_BY_ID_SQL, Integer.class, id);
        return count != null && count > 0;
    }

    public void addLike(Long filmId, Long userId) {
        jdbcTemplate.update(ADD_LIKE_SQL, filmId, userId);
    }

    public void removeLike(Long filmId, Long userId) {
        jdbcTemplate.update(REMOVE_LIKE_SQL, filmId, userId);
    }

    public Set<Long> getLikes(Long filmId) {
        List<Long> likes = jdbcTemplate.queryForList(GET_LIKES_SQL, Long.class, filmId);
        return Set.copyOf(likes);
    }

    public List<Film> getPopularFilms(int count) {
        List<Film> films = jdbcTemplate.query(GET_POPULAR_SQL, rs -> {
            List<Film> result = new ArrayList<>();
            Map<Long, Film> filmMap = new HashMap<>();

            while (rs.next()) {
                Long filmId = rs.getLong("id");
                Film film = filmMap.get(filmId);

                if (film == null) {
                    film = filmRowMapper.mapRow(rs, rs.getRow());

                    MpaRating mpa = new MpaRating();
                    mpa.setId(rs.getLong("mpa_id"));
                    mpa.setName(rs.getString("mpa_name"));
                    mpa.setDescription(rs.getString("mpa_description"));
                    film.setMpa(mpa);

                    filmMap.put(filmId, film);
                    result.add(film);
                }
            }

            if (!filmMap.isEmpty()) {
                loadFilmsData(filmMap);
            }

            return result;
        }, count);

        return films;
    }

    private void loadFilmsData(Map<Long, Film> filmMap) {
        Set<Long> filmIds = filmMap.keySet();

        if (filmIds.isEmpty()) {
            return;
        }
        loadFilmsGenres(filmMap, filmIds);
        loadFilmsLikes(filmMap, filmIds);
    }

    private void loadFilmsGenres(Map<Long, Film> filmMap, Set<Long> filmIds) {
        String inClause = String.join(",", Collections.nCopies(filmIds.size(), "?"));
        String sql = String.format(GET_FILMS_GENRES_SQL, inClause);

        jdbcTemplate.query(sql, rs -> {
            Long filmId = rs.getLong("film_id");
            Film film = filmMap.get(filmId);
            if (film != null) {
                Genre genre = genreRowMapper.mapRow(rs, rs.getRow());
                film.getGenres().add(genre);
            }
        }, filmIds.toArray());
    }

    private void loadFilmsLikes(Map<Long, Film> filmMap, Set<Long> filmIds) {
        String inClause = String.join(",", Collections.nCopies(filmIds.size(), "?"));
        String sql = String.format(GET_FILMS_LIKES_SQL, inClause);

        jdbcTemplate.query(sql, rs -> {
            Long filmId = rs.getLong("film_id");
            Long userId = rs.getLong("user_id");
            Film film = filmMap.get(filmId);
            if (film != null) {
                film.getLikes().add(userId);
            }
        }, filmIds.toArray());
    }

    private void saveGenres(Film film) {
        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            Set<Genre> uniqueGenres = new HashSet<>();
            Set<Long> seenIds = new HashSet<>();

            for (Genre genre : film.getGenres()) {
                if (genre != null && genre.getId() != null && !seenIds.contains(genre.getId())) {
                    uniqueGenres.add(genre);
                    seenIds.add(genre.getId());
                }
            }

            List<Genre> genreList = new ArrayList<>(uniqueGenres);
            jdbcTemplate.batchUpdate(ADD_GENRE_SQL, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    ps.setLong(1, film.getId());
                    ps.setLong(2, genreList.get(i).getId());
                }

                @Override
                public int getBatchSize() {
                    return genreList.size();
                }
            });
        }
    }

    private void updateGenres(Film film) {
        jdbcTemplate.update(REMOVE_GENRES_SQL, film.getId());
        saveGenres(film);
    }
}