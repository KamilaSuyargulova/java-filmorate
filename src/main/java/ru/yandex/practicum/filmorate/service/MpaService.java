package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.storage.mpa.MpaRatingStorage;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MpaService {
    private final MpaRatingStorage mpaRatingStorage;

    public List<MpaRating> getAllMpa() {
        return mpaRatingStorage.findAll();
    }

    public MpaRating getMpaById(Long id) {
        return mpaRatingStorage.findById(id)
                .orElseThrow(() -> new NotFoundException("Рейтинг MPA с id=" + id + " не найден"));
    }

    public MpaRating getDefaultMpa() {
        MpaRating defaultMpa = new MpaRating();
        defaultMpa.setId(1L);
        defaultMpa.setName("G");
        defaultMpa.setDescription("у фильма нет возрастных ограничений");
        return defaultMpa;
    }
}