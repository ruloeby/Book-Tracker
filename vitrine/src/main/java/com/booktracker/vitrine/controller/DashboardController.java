package com.booktracker.vitrine.controller;

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

import java.util.*;

@Controller
public class DashboardController {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${book.service.url}")
    private String bookServiceUrl;

    @GetMapping("/dashboard")
    public String dashboard(HttpServletRequest request, Model model) {
        String token = extractTokenFromCookie(request);

        if (token == null || !jwtUtil.validateToken(token)) {
            return "redirect:/login";
        }

        String username = jwtUtil.extractUsername(token);
        String email = jwtUtil.extractEmail(token);
        String keycloakSubject = jwtUtil.extractSubject(token);

        Map<String, Object> user = findOrCreateUser(token, keycloakSubject, email, username);
        Long userId = Long.valueOf(user.get("id").toString());

        model.addAttribute("user", user);

        try {
            List<Map<String, Object>> userLibrary = fetchUserLibrary(token, userId);
            List<Map<String, Object>> enrichedLibrary = enrichLibraryData(userLibrary, token, userId);
            model.addAttribute("userBooks", enrichedLibrary);

            Map<String, Object> stats = fetchUserStats(token, userId);

            List<Map<String, Object>> userRatings = fetchUserRatings(token, userId);
            stats.put("totalRatings", userRatings != null ? userRatings.size() : 0);

            model.addAttribute("stats", stats);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            model.addAttribute("userBooks", List.of());
            model.addAttribute("stats", createEmptyStats());
        }

        return "dashboard";
    }

    @PostMapping("/dashboard/rate-book")
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

    @PutMapping("/dashboard/library/{libraryId}/progress")
    @ResponseBody
    public Map<String, Object> updateProgress(
            @PathVariable Long libraryId,
            @RequestBody Map<String, Object> progressData,
            HttpServletRequest request) {

        String token = extractTokenFromCookie(request);
        if (token == null || !jwtUtil.validateToken(token)) {
            return Map.of("success", false, "message", "Not authenticated");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(token);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(progressData, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    bookServiceUrl + "/api/v1/library/" + libraryId + "/progress",
                    HttpMethod.PUT,
                    entity,
                    Map.class
            );

            return Map.of("success", true, "data", response.getBody());
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    @PutMapping("/dashboard/library/{libraryId}/status")
    @ResponseBody
    public Map<String, Object> updateStatus(
            @PathVariable Long libraryId,
            @RequestBody Map<String, String> statusData,
            HttpServletRequest request) {

        String token = extractTokenFromCookie(request);
        if (token == null || !jwtUtil.validateToken(token)) {
            return Map.of("success", false, "message", "Not authenticated");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(token);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(statusData, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    bookServiceUrl + "/api/v1/library/" + libraryId + "/status",
                    HttpMethod.PUT,
                    entity,
                    Map.class
            );

            return Map.of("success", true, "data", response.getBody());
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("success", false, "message", e.getMessage());
        }
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

    private List<Map<String, Object>> fetchUserLibrary(String token, Long userId) {
        try {
            String url = bookServiceUrl + "/api/v1/library/users/" + userId;

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<List<Map<String, Object>>> resp = restTemplate.exchange(
                    url, HttpMethod.GET, entity,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );

            return resp.getBody() != null ? resp.getBody() : List.of();
        } catch (Exception e) {
            System.err.println("Error fetching library: " + e.getMessage());
            return List.of();
        }
    }

    private List<Map<String, Object>> fetchUserRatings(String token, Long userId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<List<Map<String, Object>>> resp = restTemplate.exchange(
                    bookServiceUrl + "/api/v1/ratings/users/" + userId,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );

            return resp.getBody();
        } catch (Exception e) {
            System.err.println("Error fetching ratings: " + e.getMessage());
            return List.of();
        }
    }

    private Map<String, Object> fetchUserStats(String token, Long userId) {
        try {
            String url = bookServiceUrl + "/api/v1/library/users/" + userId + "/stats";
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> resp = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                Map<String, Object> body = resp.getBody();
                if (body.containsKey("stats")) {
                    return new HashMap<>((Map<String, Object>) body.get("stats"));
                }
                return new HashMap<>(body);
            }
            return createEmptyStats();
        } catch (Exception e) {
            System.err.println("Error fetching stats: " + e.getMessage());
            return createEmptyStats();
        }
    }

    private List<Map<String, Object>> enrichLibraryData(List<Map<String, Object>> libraryData,
                                                        String token, Long userId) {
        Map<Long, Integer> bookRatings = new HashMap<>();
        try {
            List<Map<String, Object>> ratings = fetchUserRatings(token, userId);
            if (ratings != null) {
                for (Map<String, Object> rating : ratings) {
                    if (rating.get("book") != null) {
                        Map<String, Object> book = (Map<String, Object>) rating.get("book");
                        Long bookId = Long.valueOf(book.get("id").toString());
                        Integer ratingValue = Integer.valueOf(rating.get("rating").toString());
                        bookRatings.put(bookId, ratingValue);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching user ratings: " + e.getMessage());
        }

        List<Map<String, Object>> enriched = new ArrayList<>();
        for (Map<String, Object> item : libraryData) {
            Map<String, Object> enrichedItem = new HashMap<>(item);

            Object libraryId = item.get("id");
            enrichedItem.put("libraryId", libraryId);

            if (item.containsKey("book") && item.get("book") != null) {
                Map<String, Object> book = (Map<String, Object>) item.get("book");
                enrichedItem.put("title", book.get("title"));
                enrichedItem.put("author", book.get("author"));
                enrichedItem.put("coverUrl", book.get("coverUrl"));
                enrichedItem.put("totalPages", book.get("totalPages"));
                enrichedItem.put("bookId", book.get("id"));

                Long bookId = Long.valueOf(book.get("id").toString());
                enrichedItem.put("userRating", bookRatings.getOrDefault(bookId, 0));
            }

            if (item.containsKey("status")) {
                enrichedItem.put("statusDisplay", getStatusDisplay(item.get("status").toString()));
            }

            if (item.containsKey("readingProgress") && item.get("readingProgress") != null) {
                Map<String, Object> progress = (Map<String, Object>) item.get("readingProgress");
                enrichedItem.putAll(progress);
            }

            enriched.add(enrichedItem);
        }
        return enriched;
    }

    private String getStatusDisplay(String status) {
        return switch (status) {
            case "TO_READ" -> "To Read";
            case "READING" -> "Reading";
            case "COMPLETED" -> "Completed";
            case "ON_HOLD" -> "On Hold";
            case "DROPPED" -> "Dropped";
            default -> status;
        };
    }

    private Map<String, Object> createEmptyStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalBooks", 0);
        stats.put("readingBooks", 0);
        stats.put("completedBooks", 0);
        stats.put("toReadBooks", 0);
        stats.put("totalRatings", 0);
        return stats;
    }

    @PostMapping("/dashboard/add-book")
    @ResponseBody
    public Map<String, Object> addBook(@RequestBody Map<String, Object> bookData, HttpServletRequest request) {
        String token = extractTokenFromCookie(request);
        if (token == null || !jwtUtil.validateToken(token)) {
            return Map.of("success", false, "message", "Not authenticated");
        }

        String keycloakSubject = jwtUtil.extractSubject(token);
        String email = jwtUtil.extractEmail(token);
        String username = jwtUtil.extractUsername(token);

        try {
            Map<String, Object> user = findOrCreateUser(token, keycloakSubject, email, username);
            Long userId = Long.valueOf(user.get("id").toString());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(token);

            HttpEntity<Map<String, Object>> createRequest = new HttpEntity<>(bookData, headers);
            ResponseEntity<Map> createResp = restTemplate.postForEntity(
                    bookServiceUrl + "/api/v1/books",
                    createRequest,
                    Map.class
            );

            if (!createResp.getStatusCode().is2xxSuccessful() || createResp.getBody() == null) {
                return Map.of("success", false, "message", "Failed to create book");
            }

            Long bookId = Long.valueOf(createResp.getBody().get("id").toString());

            Map<String, Object> libPayload = new HashMap<>();
            libPayload.put("userId", userId);
            libPayload.put("bookId", bookId);
            libPayload.put("status", bookData.getOrDefault("status", "TO_READ"));

            HttpEntity<Map<String, Object>> libRequest = new HttpEntity<>(libPayload, headers);
            ResponseEntity<Map> libResp = restTemplate.postForEntity(
                    bookServiceUrl + "/api/v1/library",
                    libRequest,
                    Map.class
            );

            if (libResp.getStatusCode().is2xxSuccessful()) {
                return Map.of("success", true, "message", "Book added", "bookId", bookId);
            } else {
                return Map.of("success", false, "message", "Failed to add to library");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    @DeleteMapping("/dashboard/remove-book/{libraryId}")
    @ResponseBody
    public Map<String, Object> removeBook(@PathVariable Long libraryId, HttpServletRequest request) {
        String token = extractTokenFromCookie(request);
        if (token == null) {
            return Map.of("success", false, "message", "Not authenticated");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            restTemplate.exchange(
                    bookServiceUrl + "/api/v1/library/" + libraryId,
                    HttpMethod.DELETE,
                    entity,
                    Void.class
            );
            return Map.of("success", true);
        } catch (Exception e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }
}