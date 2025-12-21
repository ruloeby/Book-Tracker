package com.booktracker.vitrine.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;

@Service
public class BookSummaryFlaskService {

    private final RestTemplate rest = new RestTemplate();

    @Value("${ai.service.url}")
    private String aiServiceUrl;

    public String getBookSummary(String title, String author) {
        Map<String, Object> body = new HashMap<>();
        body.put("title", title);
        body.put("author", author);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String,Object>> request = new HttpEntity<>(body, headers);

            String url = aiServiceUrl + "/bookSummary";
            Map response = rest.postForObject(url, request, Map.class);

            if (response != null && response.containsKey("summary")) {
                return response.get("summary").toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "Summary error";
    }
}