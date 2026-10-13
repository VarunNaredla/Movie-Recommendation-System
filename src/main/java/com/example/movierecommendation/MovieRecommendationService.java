package com.example.movierecommendation;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class MovieRecommendationService {

    private final List<Movie> catalog;

    public MovieRecommendationService() {

        this.catalog = List.of(

                new Movie("Inception", 8.8, "Sci-Fi", "Thriller", "Action"),

                new Movie("Interstellar", 8.6, "Sci-Fi", "Drama"),

                new Movie("They Call Him OG", 8.7, "Gangster Drama", "Action"),

                new Movie("Titanic", 7.9, "Romance", "Drama"),

                new Movie("The Notebook", 7.8, "Romance", "Drama"),

                new Movie("Joker", 8.5, "Crime", "Drama", "Thriller"),

                new Movie("Parasite", 8.5, "Thriller", "Drama", "Mystery"),

                new Movie("The Dark Knight", 9.0, "Action", "Crime", "Drama")
        );
    }

    public List<Movie> recommend(String title, int limit) {

        if (limit <= 0) {
            return List.of();
        }

        String normalizedTitle =
                title == null ? "" : title.trim();

        if (normalizedTitle.isBlank()) {
            throw new IllegalArgumentException(
                    "Movie title cannot be blank"
            );
        }

        Movie base = catalog.stream()
                .filter(movie ->
                        movie.getTitle()
                                .equalsIgnoreCase(normalizedTitle))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Movie not found: " + normalizedTitle
                        ));

        Set<String> baseGenres =
                new LinkedHashSet<>(base.getGenres());

        return catalog.stream()

                .filter(movie ->
                        !movie.getTitle()
                                .equalsIgnoreCase(normalizedTitle))

                .map(movie ->
                        new ScoredMovie(
                                movie,
                                score(baseGenres, movie)
                        ))

                .sorted(
                        Comparator.comparingDouble(
                                ScoredMovie::score
                        ).reversed()

                        .thenComparing(
                                scored ->
                                        scored.movie().getTitle()
                        )
                )

                .limit(limit)

                .map(ScoredMovie::movie)

                .toList();
    }

    private double score(Set<String> baseGenres,
                         Movie movie) {

        Set<String> otherGenres =
                new LinkedHashSet<>(movie.getGenres());

        int overlap = (int) baseGenres.stream()
                .filter(otherGenres::contains)
                .count();

        int total =
                baseGenres.size()
                        + otherGenres.size()
                        - overlap;

        double genreScore =
                total == 0
                        ? 0
                        : (double) overlap / total;

        double ratingBoost =
                movie.getRating() / 10.0;

        return genreScore * 0.85
                + ratingBoost * 0.15;
    }

    public List<Movie> getCatalog() {
        return new ArrayList<>(catalog);
    }

    private record ScoredMovie(Movie movie,
                               double score) {
    }
}