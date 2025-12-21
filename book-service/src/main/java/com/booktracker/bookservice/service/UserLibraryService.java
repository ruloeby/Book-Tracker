package com.booktracker.bookservice.service;

import com.booktracker.bookservice.entity.*;
import com.booktracker.bookservice.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class UserLibraryService {

    @Autowired
    private UserLibraryRepository libraryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private ReadingProgressRepository progressRepository;

    public UserLibrary addBookToLibrary(Long userId, Long bookId, ReadingStatus status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Book not found"));

        UserLibrary library = new UserLibrary();
        library.setUser(user);
        library.setBook(book);
        library.setStatus(status);
        library.setAddedAt(LocalDateTime.now());

        UserLibrary savedLibrary = libraryRepository.save(library);

        ReadingProgress progress = new ReadingProgress(savedLibrary);
        progressRepository.save(progress);
        savedLibrary.setReadingProgress(progress);

        return savedLibrary;
    }

    public List<UserLibrary> getUserLibrary(Long userId) {
        return libraryRepository.findByUserId(userId);
    }

    public List<UserLibrary> getBooksByStatus(Long userId, ReadingStatus status) {
        return libraryRepository.findByUserIdAndStatus(userId, status);
    }

    public UserLibrary getLibraryEntry(Long libraryId) {
        return libraryRepository.findById(libraryId).orElse(null);
    }

    /**
     * Helper method to mark a book as completed
     * Automatically sets current page to total pages
     */
    private void completeBook(UserLibrary library) {
        ReadingProgress progress = library.getReadingProgress();
        if (progress != null) {
            Integer totalPages = library.getBook().getTotalPages();
            if (totalPages != null && totalPages > 0) {
                progress.setCurrentPage(totalPages);
                progress.setProgressPercent(100.0);
                if (progress.getCompletedAt() == null) {
                    progress.setCompletedAt(LocalDateTime.now());
                }
                progress.setLastUpdated(LocalDateTime.now());
            }
        }
        library.setStatus(ReadingStatus.COMPLETED);
    }

    /**
     * Update the reading status of a library entry
     * If status is COMPLETED, automatically sets current page to total pages
     */
    public UserLibrary updateStatus(Long libraryId, ReadingStatus newStatus) {
        UserLibrary library = libraryRepository.findById(libraryId)
                .orElseThrow(() -> new RuntimeException("Library entry not found"));

        if (newStatus == ReadingStatus.COMPLETED) {
            completeBook(library);
        } else {
            library.setStatus(newStatus);
        }

        return libraryRepository.save(library);
    }

    public void removeBookFromLibrary(Long libraryId) {
        libraryRepository.deleteById(libraryId);
    }

    public ReadingProgress updateProgress(Long libraryId, Integer currentPage, String notes) {
        System.out.println("=== UserLibraryService.updateProgress ===");
        System.out.println("libraryId: " + libraryId + ", currentPage: " + currentPage);

        UserLibrary library = libraryRepository.findById(libraryId)
                .orElseThrow(() -> new RuntimeException("Library entry not found: " + libraryId));

        System.out.println("Found library entry: " + library.getId());

        ReadingProgress progress = library.getReadingProgress();
        if (progress == null) {
            System.out.println("Creating new ReadingProgress");
            progress = new ReadingProgress(library);
        }

        progress.setCurrentPage(currentPage);
        if (notes != null) {
            progress.setNotes(notes);
        }
        progress.calculateProgress();

        System.out.println("Saving progress...");
        return progressRepository.save(progress);
    }

    public ReadingProgress updateProgressWithTotal(Long libraryId, Integer currentPage, Integer totalPages, String notes) {
        UserLibrary library = libraryRepository.findById(libraryId)
                .orElseThrow(() -> new RuntimeException("Library entry not found"));

        Book book = library.getBook();
        if (totalPages != null) {
            book.setTotalPages(totalPages);
            bookRepository.save(book);
        }

        ReadingProgress progress = library.getReadingProgress();
        if (progress == null) {
            progress = new ReadingProgress(library);
        }

        progress.setCurrentPage(currentPage);
        if (notes != null) {
            progress.setNotes(notes);
        }
        progress.calculateProgress();

        return progressRepository.save(progress);
    }

    public ReadingProgress getProgress(Long libraryId) {
        UserLibrary library = libraryRepository.findById(libraryId)
                .orElseThrow(() -> new RuntimeException("Library entry not found"));
        return library.getReadingProgress();
    }

    public Map<String, Object> getUserStats(Long userId) {
        List<UserLibrary> allBooks = libraryRepository.findByUserId(userId);

        long totalBooks = allBooks.size();
        long readingBooks = allBooks.stream()
                .filter(lib -> lib.getStatus() == ReadingStatus.READING)
                .count();
        long completedBooks = allBooks.stream()
                .filter(lib -> lib.getStatus() == ReadingStatus.COMPLETED)
                .count();
        long toReadBooks = allBooks.stream()
                .filter(lib -> lib.getStatus() == ReadingStatus.TO_READ)
                .count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalBooks", totalBooks);
        stats.put("readingBooks", readingBooks);
        stats.put("completedBooks", completedBooks);
        stats.put("toReadBooks", toReadBooks);

        return stats;
    }
}