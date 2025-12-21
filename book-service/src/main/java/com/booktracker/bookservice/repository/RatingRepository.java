package com.booktracker.bookservice.repository;

import com.booktracker.bookservice.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {

    List<Rating> findByUserId(Long userId);

    List<Rating> findByBookId(Long bookId);

    Optional<Rating> findByUserIdAndBookId(Long userId, Long bookId);
}