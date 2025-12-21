package com.booktracker.bookservice.repository;

import com.booktracker.bookservice.entity.ReadingStatus;
import com.booktracker.bookservice.entity.UserLibrary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserLibraryRepository extends JpaRepository<UserLibrary, Long> {

    List<UserLibrary> findByUserId(Long userId);

    List<UserLibrary> findByUserIdAndStatus(Long userId, ReadingStatus status);

    Optional<UserLibrary> findByUserIdAndBookId(Long userId, Long bookId);

    boolean existsByUserIdAndBookId(Long userId, Long bookId);

    @Query("SELECT COUNT(ul) FROM UserLibrary ul WHERE ul.user.id = :userId")
    Long countByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(ul) FROM UserLibrary ul WHERE ul.user.id = :userId AND ul.status = :status")
    Long countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") ReadingStatus status);

    @Query("SELECT ul.book.author FROM UserLibrary ul WHERE ul.user.id = :userId GROUP BY ul.book.author ORDER BY COUNT(ul.book.author) DESC")
    List<String> findMostReadAuthorsByUser(@Param("userId") Long userId);

    @Query("SELECT ul FROM UserLibrary ul WHERE ul.user.id = :userId ORDER BY ul.addedAt DESC")
    List<UserLibrary> findByUserIdOrderByAddedAtDesc(@Param("userId") Long userId);
}