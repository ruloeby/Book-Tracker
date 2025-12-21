package com.booktracker.vitrine.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LibraryServiceClient {

    private final RestTemplate restTemplate;

    @Value("${book.service.url}")
    private String bookServiceUrl;

    public LibraryServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Map<String, Object> addBookToLibrary(Long userId, Long bookId, String status) {
        String url = bookServiceUrl + "/api/v1/library";  // CHANGED

        Map<String, Object> request = new HashMap<>();
        request.put("userId", userId);
        request.put("bookId", bookId);
        request.put("status", status);

        return restTemplate.postForObject(url, request, Map.class);
    }

    public List<Map<String, Object>> getUserLibrary(Long userId) {
        String url = bookServiceUrl + "/api/v1/library/users/" + userId;  // CHANGED

        return restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
        ).getBody();
    }

    public List<Map<String, Object>> getBooksByStatus(Long userId, String status) {
        String url = bookServiceUrl + "/api/v1/library/users/" + userId + "/status/" + status;  // CHANGED

        return restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
        ).getBody();
    }

    public Map<String, Object> updateStatus(Long libraryId, String status) {
        String url = bookServiceUrl + "/api/v1/library/" + libraryId + "/status";  // CHANGED

        Map<String, String> request = new HashMap<>();
        request.put("status", status);

        return restTemplate.exchange(
                url,
                HttpMethod.PUT,
                new org.springframework.http.HttpEntity<>(request),
                Map.class
        ).getBody();
    }

    public Map<String, Object> updateProgress(Long libraryId, Integer currentPage, Integer totalPages, String notes) {
        String url = bookServiceUrl + "/api/v1/library/" + libraryId + "/progress";  // CHANGED

        Map<String, Object> request = new HashMap<>();
        request.put("currentPage", currentPage);
        request.put("totalPages", totalPages);
        if (notes != null && !notes.trim().isEmpty()) {
            request.put("notes", notes);
        }

        return restTemplate.exchange(
                url,
                HttpMethod.PUT,
                new org.springframework.http.HttpEntity<>(request),
                Map.class
        ).getBody();
    }

    public Map<String, Object> getProgress(Long libraryId) {
        String url = bookServiceUrl + "/api/v1/library/" + libraryId + "/progress";  // CHANGED
        return restTemplate.getForObject(url, Map.class);
    }

    public void removeBookFromLibrary(Long libraryId) {
        String url = bookServiceUrl + "/api/v1/library/" + libraryId;  // CHANGED
        restTemplate.delete(url);
    }

    public Map<String, Object> getUserStats(Long userId) {
        String url = bookServiceUrl + "/api/v1/library/users/" + userId + "/stats";  // CHANGED
        return restTemplate.getForObject(url, Map.class);
    }
}