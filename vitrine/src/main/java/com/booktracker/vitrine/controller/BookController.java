package com.booktracker.vitrine.controller;

import com.booktracker.vitrine.service.AITranslateService;
import com.booktracker.vitrine.service.BookServiceClient;
import com.booktracker.vitrine.service.BookSummaryFlaskService;
import com.booktracker.vitrine.util.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class BookController {

    private final BookServiceClient bookService;
    private final AITranslateService aiTranslateService;
    private final BookSummaryFlaskService bookSummaryFlaskService;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${book.service.url}")
    private String bookServiceUrl;

    @Value("${ai.service.url}")
    private String aiServiceUrl;

    public BookController(BookServiceClient bookService, AITranslateService aiTranslateService,
                          BookSummaryFlaskService bookSummaryFlaskService) {
        this.bookService = bookService;
        this.aiTranslateService = aiTranslateService;
        this.bookSummaryFlaskService = bookSummaryFlaskService;
    }

    @GetMapping({"/", "/home"})
    public String home() {
        return "home";
    }

    @GetMapping("/books")
    public String books(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String genre,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "24") int size,
            HttpServletRequest request,
            Model model) {

        boolean isLoggedIn = isUserLoggedIn(request);
        model.addAttribute("isLoggedIn", isLoggedIn);

        Long userId = null;
        if (isLoggedIn) {
            String token = extractTokenFromCookie(request);
            String keycloakSubject = jwtUtil.extractSubject(token);
            String email = jwtUtil.extractEmail(token);
            String username = jwtUtil.extractUsername(token);

            Map<String, Object> user = findOrCreateUser(token, keycloakSubject, email, username);
            userId = Long.valueOf(user.get("id").toString());
            model.addAttribute("userId", userId);
            model.addAttribute("user", user);

            model.addAttribute("recommendations", List.of());
            model.addAttribute("hasRecommendations", false);
        }

        try {
            String searchQuery = (q != null && !q.isEmpty()) ? q : "popular";
            if (genre != null && !genre.isEmpty()) {
                searchQuery = genre;
            }

            String url = "https://openlibrary.org/search.json?q=" + searchQuery +
                    "&page=" + page + "&limit=" + size;

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getBody() != null) {
                List<Map<String, Object>> books = (List<Map<String, Object>>) response.getBody().get("docs");
                int numFound = (Integer) response.getBody().getOrDefault("numFound", 0);

                model.addAttribute("books", books);
                model.addAttribute("query", q);
                model.addAttribute("selectedGenre", genre);
                model.addAttribute("currentPage", page);
                model.addAttribute("pageSize", size);
                model.addAttribute("hasMore", numFound > page * size);
            }
        } catch (Exception e) {
            System.err.println("Error fetching books: " + e.getMessage());
            model.addAttribute("books", List.of());
        }

        model.addAttribute("query", q);
        model.addAttribute("selectedGenre", genre);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);

        return "books";
    }

    @GetMapping("/api/recommendations")
    @ResponseBody
    public Map<String, Object> getRecommendationsAsync(HttpServletRequest request) {
        String token = extractTokenFromCookie(request);
        if (token == null || !jwtUtil.validateToken(token)) {
            return Map.of("success", false, "recommendations", List.of(), "hasBooks", false);
        }

        try {
            String keycloakSubject = jwtUtil.extractSubject(token);
            String email = jwtUtil.extractEmail(token);
            String username = jwtUtil.extractUsername(token);

            Map<String, Object> user = findOrCreateUser(token, keycloakSubject, email, username);
            Long userId = Long.valueOf(user.get("id").toString());

            List<Map<String, Object>> libraryData = getUserLibraryTitlesWithSourceInternal(token, userId);
            List<String> apiBookTitles = libraryData.stream()
                    .filter(item -> "api".equals(item.get("source")))
                    .map(item -> (String) item.get("title"))
                    .toList();

            if (apiBookTitles.isEmpty()) {
                return Map.of(
                        "success", true,
                        "recommendations", List.of(),
                        "hasBooks", false,
                        "reason", "empty_library"
                );
            }

            List<Map<String, Object>> recommendations = fetchRecommendations(token, userId);

            return Map.of(
                    "success", true,
                    "recommendations", recommendations != null ? recommendations : List.of(),
                    "hasBooks", true,
                    "hasRecommendations", recommendations != null && !recommendations.isEmpty()
            );
        } catch (Exception e) {
            System.err.println("Error fetching recommendations: " + e.getMessage());
            return Map.of("success", false, "recommendations", List.of(), "hasBooks", false);
        }
    }

    @GetMapping("/vetrine")
    public String vetrine(
            @RequestParam(required = false) String query,
            HttpServletRequest request,
            Model model) {

        boolean isLoggedIn = isUserLoggedIn(request);
        model.addAttribute("isLoggedIn", isLoggedIn);

        if (query != null && !query.isEmpty()) {
            try {
                String url = "https://openlibrary.org/search.json?q=" + query + "&limit=20";
                ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

                if (response.getBody() != null) {
                    List<Map<String, Object>> books = (List<Map<String, Object>>) response.getBody().get("docs");
                    model.addAttribute("books", books);
                }
            } catch (Exception e) {
                System.err.println("Error searching books: " + e.getMessage());
                model.addAttribute("books", List.of());
            }
        }

        model.addAttribute("query", query);
        return "vitrine";
    }

    @PostMapping("/rate-book")
    @ResponseBody
    public Map<String, Object> rateBook(@RequestBody Map<String, Object> ratingData,
                                        HttpServletRequest request) {
        String token = extractTokenFromCookie(request);
        if (token == null || !jwtUtil.validateToken(token)) {
            return Map.of("success", false, "message", "Not authenticated");
        }

        try {
            String keycloakSubject = jwtUtil.extractSubject(token);
            String email = jwtUtil.extractEmail(token);
            String username = jwtUtil.extractUsername(token);

            Map<String, Object> user = findOrCreateUser(token, keycloakSubject, email, username);
            Long userId = Long.valueOf(user.get("id").toString());

            ratingData.put("userId", userId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(token);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(ratingData, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    bookServiceUrl + "/api/v1/ratings",
                    entity,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                return Map.of("success", true, "data", response.getBody());
            }
            return Map.of("success", false, "message", "Failed to save rating");

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    @GetMapping("/api/ratings/books/{bookId}")
    @ResponseBody
    public Map<String, Object> getBookRating(@PathVariable Long bookId, HttpServletRequest request) {
        String token = extractTokenFromCookie(request);

        try {
            HttpHeaders headers = new HttpHeaders();
            if (token != null) {
                headers.setBearerAuth(token);
            }
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> statsResponse = restTemplate.exchange(
                    bookServiceUrl + "/api/v1/ratings/book/" + bookId + "/stats",
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            Map<String, Object> result = new HashMap<>();
            if (statsResponse.getBody() != null) {
                result.putAll(statsResponse.getBody());
            }

            if (token != null && jwtUtil.validateToken(token)) {
                String keycloakSubject = jwtUtil.extractSubject(token);
                String email = jwtUtil.extractEmail(token);
                String username = jwtUtil.extractUsername(token);
                Map<String, Object> user = findOrCreateUser(token, keycloakSubject, email, username);
                Long userId = Long.valueOf(user.get("id").toString());

                ResponseEntity<Map> userRatingResponse = restTemplate.exchange(
                        bookServiceUrl + "/api/v1/ratings/user/" + userId + "/book/" + bookId,
                        HttpMethod.GET,
                        entity,
                        Map.class
                );

                if (userRatingResponse.getBody() != null) {
                    result.put("userRating", userRatingResponse.getBody());
                }
            }

            return result;

        } catch (Exception e) {
            return Map.of("averageRating", 0, "totalRatings", 0);
        }
    }

    @GetMapping("/api/library/titles")
    @ResponseBody
    public List<String> getUserLibraryTitles(HttpServletRequest request) {
        String token = extractTokenFromCookie(request);
        if (token == null || !jwtUtil.validateToken(token)) {
            return List.of();
        }

        try {
            String keycloakSubject = jwtUtil.extractSubject(token);
            String email = jwtUtil.extractEmail(token);
            String username = jwtUtil.extractUsername(token);

            Map<String, Object> user = findOrCreateUser(token, keycloakSubject, email, username);
            Long userId = Long.valueOf(user.get("id").toString());

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    bookServiceUrl + "/api/v1/library/users/" + userId,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<>() {}
            );

            if (response.getBody() != null) {
                return response.getBody().stream()
                        .filter(item -> item.get("book") != null)
                        .map(item -> {
                            Map<String, Object> book = (Map<String, Object>) item.get("book");
                            return (String) book.get("title");
                        })
                        .toList();
            }
        } catch (Exception e) {
            System.err.println("Error fetching library titles: " + e.getMessage());
        }
        return List.of();
    }

    private List<Map<String, Object>> fetchRecommendations(String token, Long userId) {
        try {
            System.out.println(" [FLASK] Fetching recommendations for user: " + userId);

            List<Map<String, Object>> libraryData = getUserLibraryTitlesWithSourceInternal(token, userId);

            List<String> apiBookTitles = libraryData.stream()
                    .filter(item -> "api".equals(item.get("source")))
                    .map(item -> (String) item.get("title"))
                    .toList();

            int totalBooks = libraryData.size();
            int apiBooks = apiBookTitles.size();
            int manualBooks = totalBooks - apiBooks;

            System.out.println(" [FLASK] Total books in library: " + totalBooks);
            System.out.println(" [FLASK] API-sourced books: " + apiBooks + " → " + apiBookTitles);
            System.out.println(" [FLASK] Manual books (ignored): " + manualBooks);

            if (apiBookTitles.isEmpty()) {
                System.out.println(" [FLASK] No API-sourced books - no recommendations");
                return List.of();
            }

            String url = aiServiceUrl + "/api/v1/recommendations/users/" + userId;

            Map<String, Object> requestBody = Map.of("library_titles", apiBookTitles);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            System.out.println(" [FLASK] Recommendation API Response: " + response.getStatusCode());

            if (response.getBody() != null && response.getBody().containsKey("recommendations")) {
                List<Map<String, Object>> recs = (List<Map<String, Object>>) response.getBody().get("recommendations");
                System.out.println(" [FLASK] Found " + recs.size() + " recommendations");

                if (recs != null && !recs.isEmpty()) {
                    return recs;
                }
            }

            System.out.println(" [FLASK] No valid recommendations found");
            return List.of();

        } catch (Exception e) {
            System.err.println(" [FLASK] Error fetching recommendations: " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }

    @GetMapping("/api/library/titles-with-source")
    @ResponseBody
    public List<Map<String, Object>> getUserLibraryTitlesWithSource(HttpServletRequest request) {
        String token = extractTokenFromCookie(request);
        if (token == null || !jwtUtil.validateToken(token)) {
            return List.of();
        }

        try {
            String keycloakSubject = jwtUtil.extractSubject(token);
            String email = jwtUtil.extractEmail(token);
            String username = jwtUtil.extractUsername(token);

            Map<String, Object> user = findOrCreateUser(token, keycloakSubject, email, username);
            Long userId = Long.valueOf(user.get("id").toString());

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    bookServiceUrl + "/api/v1/library/users/" + userId,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<>() {}
            );

            if (response.getBody() != null) {
                return response.getBody().stream()
                        .filter(item -> item.get("book") != null)
                        .map(item -> {
                            Map<String, Object> book = (Map<String, Object>) item.get("book");
                            Map<String, Object> result = new HashMap<>();
                            result.put("title", book.get("title"));
                            String coverUrl = (String) book.get("coverUrl");
                            result.put("source", (coverUrl != null && !coverUrl.isEmpty()) ? "api" : "manual");
                            return result;
                        })
                        .toList();
            }
        } catch (Exception e) {
            System.err.println("Error fetching library titles with source: " + e.getMessage());
        }
        return List.of();
    }

    private List<Map<String, Object>> getUserLibraryTitlesWithSourceInternal(String token, Long userId) {
        if (token == null || userId == null) {
            return List.of();
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    bookServiceUrl + "/api/v1/library/users/" + userId,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<>() {}
            );

            if (response.getBody() != null) {
                return response.getBody().stream()
                        .filter(item -> item.get("book") != null)
                        .map(item -> {
                            Map<String, Object> book = (Map<String, Object>) item.get("book");
                            Map<String, Object> result = new HashMap<>();
                            result.put("title", (String) book.get("title"));
                            String coverUrl = (String) book.get("coverUrl");
                            result.put("source", (coverUrl != null && !coverUrl.isEmpty()) ? "api" : "manual");
                            return result;
                        })
                        .toList();
            }
        } catch (Exception e) {
            System.err.println("Error fetching library with source tracking: " + e.getMessage());
        }
        return List.of();
    }

    private boolean isUserLoggedIn(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("AUTH-TOKEN".equals(cookie.getName())) {
                    String token = cookie.getValue();
                    return token != null && !token.isEmpty() && jwtUtil.validateToken(token);
                }
            }
        }
        return false;
    }

    private String extractTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("AUTH-TOKEN".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private Map<String, Object> findOrCreateUser(String token, String keycloakId, String email, String name) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            String url = bookServiceUrl + "/api/v1/users/sync";
            Map<String, String> body = new HashMap<>();
            body.put("keycloakId", keycloakId);
            body.put("email", email);
            body.put("name", name != null ? name : email.split("@")[0]);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            return response.getBody();
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> user = new HashMap<>();
            user.put("id", (long) keycloakId.hashCode());
            user.put("email", email);
            user.put("name", name);
            return user;
        }
    }

    @PostMapping("/translateText")
    @ResponseBody
    public Map<String, String> translateTextAjax(
            @RequestParam String text,
            @RequestParam(defaultValue = "ar") String target) {
        String translated = aiTranslateService.translateText(text, target);
        return Map.of("original", text, "translated", translated);
    }

    @PostMapping("/bookSummary")
    @ResponseBody
    public Map<String, String> getBookSummary(@RequestBody Map<String, String> request) {
        String title = request.get("title");
        String author = request.get("author");
        String summary = bookSummaryFlaskService.getBookSummary(title, author);
        return Map.of("summary", summary);
    }
}