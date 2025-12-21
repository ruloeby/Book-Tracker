package com.booktracker.vitrine.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class UserServiceClient {

    private final RestTemplate restTemplate;

    @Value("${book.service.url}")
    private String bookServiceUrl;

    public UserServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Map<String, Object> register(String name, String email, String password) {
        String url = bookServiceUrl + "/api/v1/users/register";  // CHANGED

        Map<String, String> request = new HashMap<>();
        request.put("name", name);
        request.put("email", email);
        request.put("password", password);

        return restTemplate.postForObject(url, request, Map.class);
    }

    public Map<String, Object> login(String email, String password) {
        String url = bookServiceUrl + "/api/v1/auth/tokens";  // CHANGED - now calls auth/tokens

        Map<String, String> credentials = new HashMap<>();
        credentials.put("email", email);
        credentials.put("password", password);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, credentials, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();

                // Handle new response structure with "data" wrapper
                if (body.containsKey("data")) {
                    return (Map<String, Object>) body.get("data");
                }

                // Fallback for old structure
                if (body.containsKey("id") || body.containsKey("email")) {
                    return body;
                }
            }
            return null;

        } catch (Exception e) {
            System.out.println("Login failed: " + e.getMessage());
            return null;
        }
    }
}