package com.booktracker.bookservice.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_library")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class UserLibrary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"library", "password"})
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Enumerated(EnumType.STRING)
    private ReadingStatus status;

    @OneToOne(mappedBy = "userLibrary", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JsonIgnoreProperties("userLibrary")
    private ReadingProgress readingProgress;

    private LocalDateTime addedAt;

    // Getters/Setters (keep existing ones)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Book getBook() { return book; }
    public void setBook(Book book) { this.book = book; }
    public ReadingStatus getStatus() { return status; }
    public void setStatus(ReadingStatus status) { this.status = status; }
    public ReadingProgress getReadingProgress() { return readingProgress; }
    public void setReadingProgress(ReadingProgress readingProgress) {
        this.readingProgress = readingProgress;
        if (readingProgress != null) {
            readingProgress.setUserLibrary(this);
        }
    }
    public LocalDateTime getAddedAt() { return addedAt; }
    public void setAddedAt(LocalDateTime addedAt) { this.addedAt = addedAt; }
}