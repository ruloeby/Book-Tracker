package com.booktracker.bookservice.controller;

import com.booktracker.bookservice.entity.ReadingProgress;
import com.booktracker.bookservice.entity.ReadingStatus;
import com.booktracker.bookservice.entity.UserLibrary;
import com.booktracker.bookservice.service.UserLibraryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/library")
@CrossOrigin(origins = "*")
public class LibraryRestController {

    @Autowired
    private UserLibraryService libraryService;

    @PostMapping
    public ResponseEntity<UserLibrary> addBook(@RequestBody Map<String, Object> request) {
        try {
            System.out.println("=== Add Book to Library ===");
            System.out.println("Request: " + request);

            // Accept userId directly as Long (real database ID)
            Long userId;
            Object userIdObj = request.get("userId");
            if (userIdObj instanceof Integer) {
                userId = ((Integer) userIdObj).longValue();
            } else if (userIdObj instanceof Long) {
                userId = (Long) userIdObj;
            } else {
                userId = Long.valueOf(userIdObj.toString());
            }

            Long bookId = Long.valueOf(request.get("bookId").toString());
            String statusStr = request.getOrDefault("status", "TO_READ").toString();
            ReadingStatus status = ReadingStatus.valueOf(statusStr);

            System.out.println("userId: " + userId + ", bookId: " + bookId + ", status: " + status);

            UserLibrary library = libraryService.addBookToLibrary(userId, bookId, status);

            URI location = ServletUriComponentsBuilder
                    .fromCurrentRequest()
                    .path("/{id}")
                    .buildAndExpand(library.getId())
                    .toUri();

            return ResponseEntity.created(location).body(library);
        } catch (Exception e) {
            System.err.println("Error adding book to library: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<List<UserLibrary>> getUserLibrary(@PathVariable Long userId) {
        List<UserLibrary> library = libraryService.getUserLibrary(userId);
        return ResponseEntity.ok(library);
    }

    @GetMapping("/users/{userId}/status/{status}")
    public ResponseEntity<List<UserLibrary>> getBooksByStatus(
            @PathVariable String userId,
            @PathVariable String status) {
        try {
            Long numericUserId = (long) userId.hashCode();
            ReadingStatus readingStatus = ReadingStatus.valueOf(status.toUpperCase());
            List<UserLibrary> books = libraryService.getBooksByStatus(numericUserId, readingStatus);
            return ResponseEntity.ok(books);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{libraryId}")
    public ResponseEntity<UserLibrary> getLibraryEntry(@PathVariable Long libraryId) {
        try {
            UserLibrary library = libraryService.getLibraryEntry(libraryId);
            if (library == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(library);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{libraryId}/status")
    public ResponseEntity<UserLibrary> updateStatus(
            @PathVariable Long libraryId,
            @RequestBody Map<String, String> request) {
        try {
            ReadingStatus newStatus = ReadingStatus.valueOf(request.get("status"));
            UserLibrary updated = libraryService.updateStatus(libraryId, newStatus);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{libraryId}/status")
    public ResponseEntity<UserLibrary> updateStatusPut(
            @PathVariable Long libraryId,
            @RequestBody Map<String, String> request) {
        try {
            ReadingStatus newStatus = ReadingStatus.valueOf(request.get("status"));
            UserLibrary updated = libraryService.updateStatus(libraryId, newStatus);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{libraryId}")
    public ResponseEntity<Void> removeBook(@PathVariable Long libraryId) {
        try {
            libraryService.removeBookFromLibrary(libraryId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{libraryId}/progress")
    public ResponseEntity<ReadingProgress> updateProgress(
            @PathVariable Long libraryId,
            @RequestBody Map<String, Object> request) {
        try {
            Integer currentPage = Integer.valueOf(request.get("currentPage").toString());
            Integer totalPages = request.containsKey("totalPages") ?
                    Integer.valueOf(request.get("totalPages").toString()) : null;
            String notes = request.getOrDefault("notes", "").toString();

            ReadingProgress progress;
            if (totalPages != null) {
                progress = libraryService.updateProgressWithTotal(libraryId, currentPage, totalPages, notes);
            } else {
                progress = libraryService.updateProgress(libraryId, currentPage, notes);
            }

            return ResponseEntity.ok(progress);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{libraryId}/progress")
    public ResponseEntity<?> updateProgressPut(
            @PathVariable Long libraryId,
            @RequestBody Map<String, Object> request) {

        System.out.println("=== Book Service - Update Progress ===");
        System.out.println("Library ID: " + libraryId);
        System.out.println("Request body: " + request);

        try {
            Integer currentPage = 0;
            if (request.get("currentPage") != null) {
                currentPage = Integer.valueOf(request.get("currentPage").toString());
            }

            Integer totalPages = null;
            if (request.containsKey("totalPages") && request.get("totalPages") != null
                    && !request.get("totalPages").toString().isEmpty()) {
                totalPages = Integer.valueOf(request.get("totalPages").toString());
            }

            String notes = "";
            if (request.containsKey("notes") && request.get("notes") != null) {
                notes = request.get("notes").toString();
            }

            System.out.println("Parsed - currentPage: " + currentPage + ", totalPages: " + totalPages + ", notes: " + notes);

            ReadingProgress progress;
            if (totalPages != null) {
                progress = libraryService.updateProgressWithTotal(libraryId, currentPage, totalPages, notes);
            } else {
                progress = libraryService.updateProgress(libraryId, currentPage, notes);
            }

            System.out.println("Progress updated successfully");
            return ResponseEntity.ok(progress);

        } catch (RuntimeException e) {
            System.err.println("Runtime error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("Error updating progress: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{libraryId}/progress")
    public ResponseEntity<ReadingProgress> getProgress(@PathVariable Long libraryId) {
        try {
            ReadingProgress progress = libraryService.getProgress(libraryId);
            if (progress == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(progress);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/users/{userId}/stats")
    public ResponseEntity<Map<String, Object>> getUserStats(@PathVariable Long userId) {
        try {
            Map<String, Object> stats = libraryService.getUserStats(userId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}