package com.booktracker.bookservice.service;

import com.booktracker.bookservice.entity.Book;
import com.booktracker.bookservice.entity.Rating;
import com.booktracker.bookservice.entity.User;
import com.booktracker.bookservice.repository.BookRepository;
import com.booktracker.bookservice.repository.RatingRepository;
import com.booktracker.bookservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class RatingService {

    @Autowired
    private RatingRepository ratingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    /**
     * Create or update a book rating
     */
    public Rating rateBook(Long userId, Long bookId, Integer ratingValue) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Book not found"));

        // Check if user already rated this book
        Rating rating = ratingRepository.findByUserIdAndBookId(userId, bookId)
                .orElse(new Rating());

        rating.setUser(user);
        rating.setBook(book);
        rating.setRating(ratingValue);
        rating.setRatedAt(LocalDateTime.now());

        return ratingRepository.save(rating);
    }

    /**
     * Get all ratings by a user
     */
    public List<Rating> getUserRatings(Long userId) {
        return ratingRepository.findByUserId(userId);
    }

    /**
     * Get user's rating for a specific book
     */
    public Rating getUserBookRating(Long userId, Long bookId) {
        return ratingRepository.findByUserIdAndBookId(userId, bookId).orElse(null);
    }

    /**
     * Get all ratings for a book
     */
    public List<Rating> getBookRatings(Long bookId) {
        return ratingRepository.findByBookId(bookId);
    }

    /**
     * Get rating statistics for a book
     */
    public Map<String, Object> getBookRatingStats(Long bookId) {
        List<Rating> ratings = ratingRepository.findByBookId(bookId);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRatings", ratings.size());

        if (ratings.isEmpty()) {
            stats.put("averageRating", 0.0);
        } else {
            double average = ratings.stream()
                    .mapToInt(Rating::getRating)
                    .average()
                    .orElse(0.0);
            stats.put("averageRating", Math.round(average * 10.0) / 10.0);
        }

        return stats;
    }

    /**
     * Delete a rating
     */
    public void deleteRating(Long ratingId) {
        ratingRepository.deleteById(ratingId);
    }
}