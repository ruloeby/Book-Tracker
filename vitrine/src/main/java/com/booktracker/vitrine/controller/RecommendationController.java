package com.booktracker.vitrine.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/recommendations")
@CrossOrigin(origins = "*")
public class RecommendationController {

    @GetMapping("/users/{userId}")
    public ResponseEntity<Map<String, Object>> getUserRecommendations(@PathVariable Long userId) {
        try {
            System.out.println("=== Fetching recommendations for user: " + userId);

            List<Map<String, Object>> recommendations = generateSampleRecommendations();

            Map<String, Object> response = new HashMap<>();
            response.put("recommendations", recommendations);

            System.out.println("Returning " + recommendations.size() + " recommendations");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("Error generating recommendations: " + e.getMessage());
            e.printStackTrace();
            // Return empty array instead of failing completely
            return ResponseEntity.ok(Map.of("recommendations", generateSampleRecommendations()));
        }
    }


    private List<Map<String, Object>> generateSampleRecommendations() {
        List<Map<String, Object>> recommendations = new ArrayList<>();

        // Sample recommendation 1
        Map<String, Object> rec1 = new HashMap<>();
        rec1.put("title", "The Midnight Library");
        rec1.put("author", "Matt Haig");
        rec1.put("coverId", "10677698");
        rec1.put("reason", "Popular Fiction");
        recommendations.add(rec1);

        // Sample recommendation 2
        Map<String, Object> rec2 = new HashMap<>();
        rec2.put("title", "Project Hail Mary");
        rec2.put("author", "Andy Weir");
        rec2.put("coverId", "12662807");
        rec2.put("reason", "Sci-Fi Adventure");
        recommendations.add(rec2);

        // Sample recommendation 3
        Map<String, Object> rec3 = new HashMap<>();
        rec3.put("title", "Atomic Habits");
        rec3.put("author", "James Clear");
        rec3.put("coverId", "10452747");
        rec3.put("reason", "Self-Improvement");
        recommendations.add(rec3);



        // Sample recommendation 5
        Map<String, Object> rec5 = new HashMap<>();
        rec5.put("title", "Educated");
        rec5.put("author", "Tara Westover");
        rec5.put("coverId", "9332135");
        rec5.put("reason", "Memoir");
        recommendations.add(rec5);

        return recommendations;
    }
}