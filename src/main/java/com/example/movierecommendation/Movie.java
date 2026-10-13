package com.example.movierecommendation;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public class Movie {
    private final String title;
    private final Set<String> genres;
    private final double rating;

    public Movie(String title, double rating, String... genres) {
        this.title = title;
        this.rating = rating;
        this.genres = new LinkedHashSet<>(Arrays.asList(genres));
    }

    public String getTitle() {
        return title;
    }

    public Set<String> getGenres() {
        return genres;
    }

    public double getRating() {
        return rating;
    }
}
