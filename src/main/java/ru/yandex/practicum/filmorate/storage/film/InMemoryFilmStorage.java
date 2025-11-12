package ru.yandex.practicum.filmorate.storage.film;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.*;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@Component
public class InMemoryFilmStorage implements FilmStorage {
    private final Map<Long, Film> films = new HashMap<>();
    private long idCounter = 1;
    private static final LocalDate CINEMA_BIRTHDAY = LocalDate.of(1895, 12, 28);

    @Override
    public List<Film> findAll() {
        log.info("Получен запрос на получение всех фильмов");
        return new ArrayList<>(films.values());
    }

    @Override
    public Film create(Film film) {
        log.info("Получен запрос на добавление фильма: {}", film);
        validateFilm(film);
        film.setId(idCounter++);
        films.put(film.getId(), film);
        log.info("Добавлен новый фильм: {}", film);
        return film;
    }

    @Override
    public Film update(Film film) {
        log.info("Получен запрос на обновление фильма: {}", film);
        if (film.getId() == null || !films.containsKey(film.getId())) {
            throw new NotFoundException("Фильм с id=" + film.getId() + " не найден");
        }

        Film existingFilm = films.get(film.getId());

        if (film.getName() != null) {
            if (film.getName().isBlank()) {
                throw new ValidationException("Название не может быть пустым");
            }
            existingFilm.setName(film.getName());
        }

        if (film.getDescription() != null) {
            if (film.getDescription().length() > 200) {
                throw new ValidationException("Максимальная длина описания — 200 символов");
            }
            existingFilm.setDescription(film.getDescription());
        }

        if (film.getReleaseDate() != null) {
            if (film.getReleaseDate().isBefore(CINEMA_BIRTHDAY)) {
                throw new ValidationException("Дата релиза не может быть раньше 28 декабря 1895 года");
            }
            existingFilm.setReleaseDate(film.getReleaseDate());
        }

        if (film.getDuration() != 0) {
            if (film.getDuration() <= 0) {
                throw new ValidationException("Продолжительность фильма должна быть положительным числом");
            }
            existingFilm.setDuration(film.getDuration());
        }

        log.info("Обновлен фильм: {}", existingFilm);
        return existingFilm;
    }

    @Override
    public Optional<Film> findById(Long id) {
        return Optional.ofNullable(films.get(id));
    }

    @Override
    public void delete(Long id) {
        films.remove(id);
    }

    @Override
    public boolean existsById(Long id) {
        return films.containsKey(id);
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
        if (film.getReleaseDate().isBefore(CINEMA_BIRTHDAY)) {
            throw new ValidationException("Дата релиза не может быть раньше 28 декабря 1895 года");
        }
        if (film.getDuration() <= 0) {
            throw new ValidationException("Продолжительность фильма должна быть положительным числом");
        }
    }
}
