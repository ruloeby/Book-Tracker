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
public class AITranslateService {

    private final RestTemplate rest = new RestTemplate();

    @Value("${ai.service.url}")
    private String aiServiceUrl;

    public String translateText(String text, String target) {
        Map<String, Object> body = new HashMap<>();
        body.put("text", text);
        body.put("target", target);
        body.put("source", "en");

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            String url = aiServiceUrl + "/translateText";
            Map response = rest.postForObject(url, request, Map.class);

            if (response != null && response.containsKey("translated")) {
                return response.get("translated").toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Translation error";
    }
}