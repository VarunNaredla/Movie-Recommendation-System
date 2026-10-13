package com.example.movierecommendation;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

public class App {
    private static final MovieRecommendationService SERVICE = new MovieRecommendationService();

    public static void main(String[] args) throws IOException {
        if (args.length > 0 && args[0].equalsIgnoreCase("--cli")) {
            runCli();
            return;
        }
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/movies", exchange -> respondJson(exchange, SERVICE.getCatalog()));
        server.createContext("/api/recommend", App::recommend);
        server.createContext("/", App::serveWebAsset);
        server.start();
        System.out.println("Movie Match is ready at http://localhost:" + port);
    }

    private static void recommend(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            send(exchange, 405, "application/json; charset=utf-8", "{\"error\":\"Use GET for recommendations.\"}");
            return;
        }
        Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
        try {
            String title = query.get("title");
            int limit = Integer.parseInt(query.getOrDefault("limit", "6"));
            List<Movie> results = SERVICE.recommend(title, Math.max(1, Math.min(limit, 12)));
            String json = "{\"basedOn\":" + quote(title.trim()) + ",\"recommendations\":" + moviesJson(results) + "}";
            send(exchange, 200, "application/json; charset=utf-8", json);
        } catch (IllegalArgumentException ex) {
            send(exchange, 400, "application/json; charset=utf-8", "{\"error\":" + quote(ex.getMessage()) + "}");
        }
    }

    private static void respondJson(HttpExchange exchange, List<Movie> movies) throws IOException {
        send(exchange, 200, "application/json; charset=utf-8", moviesJson(movies));
    }

    private static String moviesJson(List<Movie> movies) {
        return movies.stream().map(movie -> "{\"title\":" + quote(movie.getTitle())
                + ",\"rating\":" + movie.getRating() + ",\"genres\":["
                + movie.getGenres().stream().map(App::quote).collect(Collectors.joining(",")) + "]}")
                .collect(Collectors.joining(",", "[", "]"));
    }

    private static String quote(String value) {
        StringBuilder out = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> { if (c < 0x20) out.append(String.format("\\u%04x", (int) c)); else out.append(c); }
            }
        }
        return out.append('"').toString();
    }

    private static Map<String, String> parseQuery(String query) {
        if (query == null || query.isBlank()) return Map.of();
        return java.util.Arrays.stream(query.split("&"))
                .map(part -> part.split("=", 2))
                .collect(Collectors.toMap(parts -> decode(parts[0]), parts -> parts.length > 1 ? decode(parts[1]) : "", (a, b) -> b));
    }

    private static String decode(String value) { return URLDecoder.decode(value, StandardCharsets.UTF_8); }

    private static void serveWebAsset(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (path.equals("/")) path = "/index.html";
        if (path.contains("..") || path.contains("\\")) {
            send(exchange, 400, "text/plain; charset=utf-8", "Invalid path"); return;
        }
        try (InputStream asset = App.class.getResourceAsStream("/web" + path)) {
            if (asset == null) { send(exchange, 404, "text/plain; charset=utf-8", "Not found"); return; }
            byte[] body = asset.readAllBytes();
            String type = path.endsWith(".css") ? "text/css" : path.endsWith(".js") ? "text/javascript" : path.endsWith(".svg") ? "image/svg+xml" : "text/html";
            exchange.getResponseHeaders().set("Content-Type", type + "; charset=utf-8");
            exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        }
    }

    private static void send(HttpExchange exchange, int status, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static void runCli() {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("Movie Recommendation System — enter a movie title:");
            try {
                for (Movie movie : SERVICE.recommend(scanner.nextLine(), 5))
                    System.out.printf("%s | %s | ★ %.1f%n", movie.getTitle(), String.join(", ", movie.getGenres()), movie.getRating());
            } catch (IllegalArgumentException ex) { System.out.println(ex.getMessage()); }
        }
    }
}
