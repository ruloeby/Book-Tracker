package com.booktracker.bookservice.dto;

import com.booktracker.bookservice.entity.*;
import java.time.LocalDateTime;

public class UserLibraryResponseDTO {
    private Long id;
    private BookDTO book;
    private ReadingStatus status;
    private ReadingProgressDTO readingProgress;
    private LocalDateTime addedAt;

    // Nested Book DTO
    public static class BookDTO {
        private Long id;
        private String title;
        private String author;
        private String coverUrl;
        private String isbn;
        private String description;
        private Integer publishYear;
        private String publisher;
        private Integer totalPages;

        public BookDTO(Book book) {
            if (book != null) {
                this.id = book.getId();
                this.title = book.getTitle();
                this.author = book.getAuthor();
                this.coverUrl = book.getCoverUrl();
                this.isbn = book.getIsbn();
                this.description = book.getDescription();
                this.publishYear = book.getPublishYear();
                this.publisher = book.getPublisher();
                this.totalPages = book.getTotalPages();
            }
        }

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }
        public String getCoverUrl() { return coverUrl; }
        public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
        public String getIsbn() { return isbn; }
        public void setIsbn(String isbn) { this.isbn = isbn; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Integer getPublishYear() { return publishYear; }
        public void setPublishYear(Integer publishYear) { this.publishYear = publishYear; }
        public String getPublisher() { return publisher; }
        public void setPublisher(String publisher) { this.publisher = publisher; }
        public Integer getTotalPages() { return totalPages; }
        public void setTotalPages(Integer totalPages) { this.totalPages = totalPages; }
    }

    // Nested ReadingProgress DTO
    public static class ReadingProgressDTO {
        private Long id;
        private Integer currentPage;
        private Integer totalPages;
        private Double progressPercent;
        private LocalDateTime lastUpdated;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
        private String notes;

        public ReadingProgressDTO(ReadingProgress progress) {
            if (progress != null) {
                this.id = progress.getId();
                this.currentPage = progress.getCurrentPage();
                this.totalPages = progress.getTotalPages();
                this.progressPercent = progress.getProgressPercent();
                this.lastUpdated = progress.getLastUpdated();
                this.startedAt = progress.getStartedAt();
                this.completedAt = progress.getCompletedAt();
                this.notes = progress.getNotes();
            }
        }

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Integer getCurrentPage() { return currentPage; }
        public void setCurrentPage(Integer currentPage) { this.currentPage = currentPage; }
        public Integer getTotalPages() { return totalPages; }
        public void setTotalPages(Integer totalPages) { this.totalPages = totalPages; }
        public Double getProgressPercent() { return progressPercent; }
        public void setProgressPercent(Double progressPercent) { this.progressPercent = progressPercent; }
        public LocalDateTime getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
        public LocalDateTime getStartedAt() { return startedAt; }
        public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
        public LocalDateTime getCompletedAt() { return completedAt; }
        public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }

    // Constructor from Entity
    public UserLibraryResponseDTO(UserLibrary library) {
        if (library != null) {
            this.id = library.getId();
            this.book = new BookDTO(library.getBook());
            this.status = library.getStatus();
            this.readingProgress = new ReadingProgressDTO(library.getReadingProgress());
            this.addedAt = library.getAddedAt();
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public BookDTO getBook() { return book; }
    public void setBook(BookDTO book) { this.book = book; }
    public ReadingStatus getStatus() { return status; }
    public void setStatus(ReadingStatus status) { this.status = status; }
    public ReadingProgressDTO getReadingProgress() { return readingProgress; }
    public void setReadingProgress(ReadingProgressDTO readingProgress) { this.readingProgress = readingProgress; }
    public LocalDateTime getAddedAt() { return addedAt; }
    public void setAddedAt(LocalDateTime addedAt) { this.addedAt = addedAt; }
}