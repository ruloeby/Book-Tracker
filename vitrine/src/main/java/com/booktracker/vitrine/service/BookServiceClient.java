
package com.booktracker.vitrine.service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

@Service
public class BookServiceClient {

    private final RestTemplate restTemplate;


    private final String OPENLIBRARY_API = "https://openlibrary.org/search.json";

    @Value("${book.service.url}")
    private String bookServiceUrl;

    public BookServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // GET /api/v1/books - Get all books
    public List<Map<String, Object>> getAllBooks() {
        String url = bookServiceUrl + "/api/v1/books";
        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
        );
        return response.getBody();
    }

    // GET /api/v1/books/{id} - Get book by ID
    public Map<String, Object> getBookById(Long id) {
        String url = bookServiceUrl + "/api/v1/books/" + id;
        return restTemplate.getForObject(url, Map.class);
    }

    // GET /api/v1/books/search?q={query} - Search books
    public List<Map<String, Object>> searchBooks(String query) {
        String url = bookServiceUrl + "/api/v1/books/search?q=" + query;
        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
        );
        return response.getBody();
    }

    // POST /api/v1/books - Create book
    public Map<String, Object> createBook(Map<String, Object> book) {
        String url = bookServiceUrl + "/api/v1/books";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(book, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
        return response.getBody();
    }

    // PUT /api/v1/books/{id} - Full update
    public Map<String, Object> updateBook(Long id, Map<String, Object> book) {
        String url = bookServiceUrl + "/api/v1/books/" + id;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(book, headers);

        restTemplate.put(url, request);
        return book;
    }

    // PATCH /api/v1/books/{id} - Partial update
    public Map<String, Object> partialUpdateBook(Long id, Map<String, Object> bookUpdates) {
        String url = bookServiceUrl + "/api/v1/books/" + id;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(bookUpdates, headers);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.PATCH, request, Map.class);
        return response.getBody();
    }

    // DELETE /api/v1/books/{id} - Delete book
    public void deleteBook(Long id) {
        String url = bookServiceUrl + "/api/v1/books/" + id;
        restTemplate.delete(url);
    }
}