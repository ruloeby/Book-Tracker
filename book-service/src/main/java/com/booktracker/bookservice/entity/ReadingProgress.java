package com.booktracker.bookservice.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reading_progress")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ReadingProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_library_id", nullable = false)
    @JsonIgnoreProperties({"readingProgress", "user"})
    private UserLibrary userLibrary;

    private Integer currentPage;

    // REMOVED: totalPages field - now always retrieved from Book entity
    private Double progressPercent;

    private LocalDateTime lastUpdated;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    private String notes;

    // Constructors
    public ReadingProgress() {
        this.lastUpdated = LocalDateTime.now();
    }

    public ReadingProgress(UserLibrary userLibrary) {
        this.userLibrary = userLibrary;
        this.currentPage = 0;
        this.progressPercent = 0.0;
        this.lastUpdated = LocalDateTime.now();
        this.startedAt = LocalDateTime.now();
    }

    // Method to calculate progress - gets totalPages from Book
    public void calculateProgress() {
        Integer totalPages = null;

        // Get totalPages from the associated Book
        if (userLibrary != null && userLibrary.getBook() != null) {
            totalPages = userLibrary.getBook().getTotalPages();
        }

        if (totalPages != null && totalPages > 0 && currentPage != null) {
            this.progressPercent = (currentPage * 100.0) / totalPages;

            // If completed (100%)
            if (this.progressPercent >= 100.0) {
                this.completedAt = LocalDateTime.now();
                this.userLibrary.setStatus(ReadingStatus.COMPLETED);
            } else if (this.progressPercent > 0) {
                this.userLibrary.setStatus(ReadingStatus.READING);
            }
        }
        this.lastUpdated = LocalDateTime.now();
    }

    // Helper method to get totalPages (for serialization)
    @Transient
    public Integer getTotalPages() {
        if (userLibrary != null && userLibrary.getBook() != null) {
            return userLibrary.getBook().getTotalPages();
        }
        return null;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UserLibrary getUserLibrary() {
        return userLibrary;
    }

    public void setUserLibrary(UserLibrary userLibrary) {
        this.userLibrary = userLibrary;
    }

    public Integer getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(Integer currentPage) {
        this.currentPage = currentPage;
        calculateProgress();
    }

    public Double getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(Double progressPercent) {
        this.progressPercent = progressPercent;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}