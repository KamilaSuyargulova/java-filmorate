package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
public class FilmService {
    @Qualifier("filmDbStorage")
    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private final MpaService mpaService;
    private final GenreService genreService;

    public FilmService(@Qualifier("filmDbStorage") FilmStorage filmStorage,
                       @Qualifier("userDbStorage") UserStorage userStorage,
                       MpaService mpaService,
                       GenreService genreService) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
        this.mpaService = mpaService;
        this.genreService = genreService;
    }

    public List<Film> findAll() {
        List<Film> films = filmStorage.findAll();
        return films;
    }

    public Film create(Film film) {
        validateFilm(film);
        MpaRating mpa;
        if (film.getMpa() == null || film.getMpa().getId() == null) {
            mpa = mpaService.getDefaultMpa();
        } else {
            mpa = mpaService.getMpaById(film.getMpa().getId());
        }
        film.setMpa(mpa);
        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            Set<Genre> validatedGenres = new LinkedHashSet<>();
            Set<Long> seenIds = new HashSet<>();

            for (Genre genre : film.getGenres()) {
                if (genre.getId() != null && !seenIds.contains(genre.getId())) {
                    Genre fullGenre = genreService.getGenreById(genre.getId());
                    validatedGenres.add(fullGenre);
                    seenIds.add(genre.getId());
                }
            }
            List<Genre> sortedGenres = new ArrayList<>(validatedGenres);
            sortedGenres.sort(Comparator.comparing(Genre::getId));

            film.setGenres(new LinkedHashSet<>(sortedGenres));
        } else {
            film.setGenres(new LinkedHashSet<>());
        }

        Film created = filmStorage.create(film);
        return created;
    }

    public Film update(Film film) {
        if (film.getId() == null) {
            throw new ValidationException("ID фильма не может быть null");
        }

        Film existingFilm = filmStorage.findById(film.getId())
                .orElseThrow(() -> new NotFoundException("Фильм с id=" + film.getId() + " не найден"));

        if (film.getName() != null) existingFilm.setName(film.getName());
        if (film.getDescription() != null) existingFilm.setDescription(film.getDescription());
        if (film.getReleaseDate() != null) existingFilm.setReleaseDate(film.getReleaseDate());
        if (film.getDuration() != 0) existingFilm.setDuration(film.getDuration());
        if (film.getMpa() != null) {
            MpaRating newMpa = mpaService.getMpaById(film.getMpa().getId());
            existingFilm.setMpa(newMpa);
        }
        if (film.getGenres() != null) {
            Set<Genre> validatedGenres = new HashSet<>();
            for (Genre genre : film.getGenres()) {
                if (genre.getId() != null) {
                    Genre fullGenre = genreService.getGenreById(genre.getId());
                    validatedGenres.add(fullGenre);
                }
            }
            existingFilm.setGenres(validatedGenres);
        }

        validateFilm(existingFilm);
        return filmStorage.update(existingFilm);
    }

    private void validateFilm(Film film) {
        if (film.getName() == null || film.getName().isBlank()) {
            throw new ValidationException("Название не может быть пустым");
        }
        if (film.getDescription() != null && film.getDescription().length() > 200) {
            throw new ValidationException("Максимальная длина описания — 200 символов");
        }
        if (film.getReleaseDate() == null) {
            throw new ValidationException("Дата релиза обязательна");
        }
        if (film.getReleaseDate().isBefore(LocalDate.of(1895, 12, 28))) {
            throw new ValidationException("Дата релиза не может быть раньше 28 декабря 1895 года");
        }
        if (film.getDuration() <= 0) {
            throw new ValidationException("Продолжительность фильма должна быть положительным числом");
        }
    }

    public Film findById(Long id) {
        Film film = filmStorage.findById(id)
                .orElseThrow(() -> new NotFoundException("Фильм с id=" + id + " не найден"));
        return film;
    }

    public void addLike(Long filmId, Long userId) {
        Film film = findById(filmId);
        if (!userStorage.existsById(userId)) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }
        filmStorage.addLike(filmId, userId);
        log.info("Пользователь {} поставил лайк фильму {}", userId, filmId);
    }

    public void removeLike(Long filmId, Long userId) {
        Film film = findById(filmId);
        if (!userStorage.existsById(userId)) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }
        filmStorage.removeLike(filmId, userId);
        log.info("Пользователь {} удалил лайк с фильма {}", userId, filmId);
    }

    public List<Film> getPopularFilms(Integer count) {
        int limit = (count == null || count <= 0) ? 10 : count;
        List<Film> films = filmStorage.getPopularFilms(limit);
        return films;
    }
}