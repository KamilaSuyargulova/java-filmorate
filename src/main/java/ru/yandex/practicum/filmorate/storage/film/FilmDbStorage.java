package ru.yandex.practicum.filmorate.storage.film;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.storage.BaseRepository;
import ru.yandex.practicum.filmorate.storage.mappers.FilmRowMapper;
import ru.yandex.practicum.filmorate.storage.mappers.GenreRowMapper;
import ru.yandex.practicum.filmorate.storage.mappers.MpaRatingRowMapper;

import java.util.*;

@Slf4j
@Repository
@Qualifier("filmDbStorage")
public class FilmDbStorage extends BaseRepository<Film> implements FilmStorage {
    private final JdbcTemplate jdbcTemplate;
    private final FilmRowMapper filmRowMapper;
    private final GenreRowMapper genreRowMapper;
    private final MpaRatingRowMapper mpaRatingRowMapper;

    private static final String FIND_ALL_SQL = "SELECT * FROM films";
    private static final String FIND_BY_ID_SQL = "SELECT * FROM films WHERE id = ?";
    private static final String INSERT_SQL = "INSERT INTO films (name, description, release_date, duration, mpa_rating_id) VALUES (?, ?, ?, ?, ?)";
    private static final String UPDATE_SQL = "UPDATE films SET name = ?, description = ?, release_date = ?, duration = ?, mpa_rating_id = ? WHERE id = ?";
    private static final String DELETE_SQL = "DELETE FROM films WHERE id = ?";
    private static final String EXISTS_BY_ID_SQL = "SELECT COUNT(*) FROM films WHERE id = ?";
    private static final String ADD_LIKE_SQL = "INSERT INTO likes (film_id, user_id) VALUES (?, ?)";
    private static final String REMOVE_LIKE_SQL = "DELETE FROM likes WHERE film_id = ? AND user_id = ?";
    private static final String GET_LIKES_SQL = "SELECT user_id FROM likes WHERE film_id = ?";
    private static final String GET_POPULAR_SQL = """
            SELECT f.*, COUNT(l.user_id) as likes_count 
            FROM films f 
            LEFT JOIN likes l ON f.id = l.film_id 
            GROUP BY f.id 
            ORDER BY likes_count DESC 
            LIMIT ?
            """;
    private static final String ADD_GENRE_SQL = "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)";
    private static final String REMOVE_GENRES_SQL = "DELETE FROM film_genres WHERE film_id = ?";
    private static final String GET_FILM_GENRES_SQL = """
            SELECT g.* FROM genres g 
            JOIN film_genres fg ON g.id = fg.genre_id 
            WHERE fg.film_id = ?
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
        List<Film> films = findMany(FIND_ALL_SQL);
        films.forEach(this::loadFilmData);
        return films;
    }

    @Override
    public Film create(Film film) {
        try {
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

        } catch (Exception e) {
            throw e;
        }
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
        Optional<Film> film = findOne(FIND_BY_ID_SQL, id);
        film.ifPresent(this::loadFilmData);
        return film;
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
        List<Film> films = jdbcTemplate.query(GET_POPULAR_SQL, filmRowMapper, count);
        films.forEach(this::loadFilmData);
        return films;
    }

    private void loadFilmData(Film film) {
        try {
            film.setLikes(getLikes(film.getId()));

            Set<Genre> genres = getFilmGenres(film.getId());
            film.setGenres(genres != null ? genres : new HashSet<>());

            MpaRating mpa = getFilmMpa(film.getId());
            film.setMpa(mpa != null ? mpa : getDefaultMpa());

        } catch (Exception e) {
            log.error("Error loading film data for film id: {}", film.getId(), e);
        }
    }

    private MpaRating getDefaultMpa() {
        MpaRating defaultMpa = new MpaRating();
        defaultMpa.setId(1L);
        defaultMpa.setName("G");
        defaultMpa.setDescription("у фильма нет возрастных ограничений");
        return defaultMpa;
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
            for (Genre genre : uniqueGenres) {
                jdbcTemplate.update(ADD_GENRE_SQL, film.getId(), genre.getId());
            }
        }
    }

    private void updateGenres(Film film) {
        jdbcTemplate.update(REMOVE_GENRES_SQL, film.getId());
        saveGenres(film);
    }

    private Set<Genre> getFilmGenres(Long filmId) {
        try {
            String sql = """
                    SELECT g.* FROM genres g 
                    JOIN film_genres fg ON g.id = fg.genre_id 
                    WHERE fg.film_id = ?
                    ORDER BY g.id ASC
                    """;

            List<Genre> genres = jdbcTemplate.query(sql, genreRowMapper, filmId);
            return new LinkedHashSet<>(genres);
        } catch (Exception e) {
            log.warn("Error loading genres for film id: {}", filmId, e);
            return new LinkedHashSet<>();
        }
    }

    private MpaRating getFilmMpa(Long filmId) {
        try {
            String sql = """
                    SELECT m.id, m.name, m.description 
                    FROM films f 
                    JOIN mpa_ratings m ON f.mpa_rating_id = m.id 
                    WHERE f.id = ?
                    """;
            return jdbcTemplate.queryForObject(sql, mpaRatingRowMapper, filmId);
        } catch (EmptyResultDataAccessException e) {
            log.warn("MPA rating not found for film id: {}, using default", filmId);
            MpaRating defaultMpa = new MpaRating();
            defaultMpa.setId(1L);
            defaultMpa.setName("G");
            defaultMpa.setDescription("у фильма нет возрастных ограничений");
            return defaultMpa;
        }
    }
}