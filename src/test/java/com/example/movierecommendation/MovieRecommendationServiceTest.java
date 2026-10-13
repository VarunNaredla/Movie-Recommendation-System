package com.example.movierecommendation;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class MovieRecommendationServiceTest {

    @Test
    void recommendsMoviesWithSimilarGenres() {
        MovieRecommendationService service = new MovieRecommendationService();

        List<Movie> recommendations = service.recommend("Inception", 5);

        assertFalse(recommendations.isEmpty(), "Expected at least one recommendation");
        assertTrue(recommendations.stream().anyMatch(movie -> movie.getTitle().equals("Interstellar")),
                "Expected Interstellar to be recommended for Inception");
    }

    @Test
    void trimsInputAndExcludesTheRequestedMovie() {
        MovieRecommendationService service = new MovieRecommendationService();

        List<Movie> recommendations = service.recommend("  inception  ", 5);

        assertFalse(recommendations.isEmpty());
        assertFalse(recommendations.stream().anyMatch(movie -> movie.getTitle().equalsIgnoreCase("Inception")));
    }

    @Test
    void returnsEmptyListForNonPositiveLimits() {
        MovieRecommendationService service = new MovieRecommendationService();

        assertEquals(0, service.recommend("Inception", 0).size());
        assertEquals(0, service.recommend("Inception", -3).size());
    }
}
