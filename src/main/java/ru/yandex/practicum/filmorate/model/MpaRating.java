package ru.yandex.practicum.filmorate.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MpaRating {
    private Long id;
    private String name;
    private String description;

    public MpaRating() {
    }

    public MpaRating(Long id, String name) {
        this.id = id;
        this.name = name;
    }
}