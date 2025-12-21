package com.booktracker.bookservice.controller;

import com.booktracker.bookservice.entity.Rating;
import com.booktracker.bookservice.service.RatingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ratings")
@CrossOrigin(origins = "*")
public class RatingRestController {

    @Autowired
    private RatingService ratingService;

    // POST /api/v1/ratings - Create or update a rating
    @PostMapping
    public ResponseEntity<Rating> rateBook(@RequestBody Map<String, Object> request) {
        try {
            System.out.println("=== Rate Book Request ===");
            System.out.println("Request: " + request);

            Long userId = Long.valueOf(request.get("userId").toString());
            Long bookId = Long.valueOf(request.get("bookId").toString());
            Integer rating = Integer.valueOf(request.get("rating").toString());

            System.out.println("userId: " + userId + ", bookId: " + bookId + ", rating: " + rating);

            Rating savedRating = ratingService.rateBook(userId, bookId, rating);

            URI location = ServletUriComponentsBuilder
                    .fromCurrentRequest()
                    .path("/{id}")
                    .buildAndExpand(savedRating.getId())
                    .toUri();

            return ResponseEntity.created(location).body(savedRating);
        } catch (Exception e) {
            System.err.println("Error rating book: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    // GET /api/v1/ratings/user/{userId} - Get all ratings by a user
    @GetMapping("/users/{userId}")
    public ResponseEntity<List<Rating>> getUserRatings(@PathVariable Long userId) {
        try {
            List<Rating> ratings = ratingService.getUserRatings(userId);
            return ResponseEntity.ok(ratings);
        } catch (Exception e) {
            System.err.println("Error fetching user ratings: " + e.getMessage());
            return ResponseEntity.ok(List.of());
        }
    }

    // GET /api/v1/ratings/user/{userId}/book/{bookId} - Get user's rating for a specific book
    @GetMapping("/users/{userId}/books/{bookId}")
    public ResponseEntity<Rating> getUserBookRating(
            @PathVariable Long userId,
            @PathVariable Long bookId) {
        try {
            Rating rating = ratingService.getUserBookRating(userId, bookId);
            if (rating == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(rating);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // GET /api/v1/ratings/book/{bookId}/stats - Get rating statistics for a book
    @GetMapping("/books/{bookId}/stats")
    public ResponseEntity<Map<String, Object>> getBookRatingStats(@PathVariable Long bookId) {
        try {
            Map<String, Object> stats = ratingService.getBookRatingStats(bookId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "averageRating", 0.0,
                    "totalRatings", 0
            ));
        }
    }

    // GET /api/v1/ratings/book/{bookId} - Get all ratings for a book
    @GetMapping("/books/{bookId}")
    public ResponseEntity<List<Rating>> getBookRatings(@PathVariable Long bookId) {
        try {
            List<Rating> ratings = ratingService.getBookRatings(bookId);
            return ResponseEntity.ok(ratings);
        } catch (Exception e) {
            return ResponseEntity.ok(List.of());
        }
    }

    // DELETE /api/v1/ratings/{ratingId} - Delete a rating
    @DeleteMapping("/{ratingId}")
    public ResponseEntity<Void> deleteRating(@PathVariable Long ratingId) {
        try {
            ratingService.deleteRating(ratingId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}