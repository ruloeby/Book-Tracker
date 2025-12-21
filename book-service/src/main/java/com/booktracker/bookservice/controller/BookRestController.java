package com.booktracker.bookservice.controller;

import com.booktracker.bookservice.entity.Book;
import com.booktracker.bookservice.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/books")
@CrossOrigin(origins = "http://localhost:8080")
public class BookRestController {

    @Autowired
    private BookService bookService;

    // GET /api/v1/books - Get all books
    @GetMapping
    public ResponseEntity<List<Book>> getAllBooks() {
        List<Book> books = bookService.findAllBooks();
        return ResponseEntity.ok(books);
    }

    // GET /api/v1/books/{id} - Get book by ID
    @GetMapping("/{id}")
    public ResponseEntity<Book> getBookById(@PathVariable Long id) {
        Book book = bookService.findBookById(id);
        if (book == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(book);
    }

    // GET /api/v1/books/search?q={query} - Search books
    @GetMapping("/search")
    public ResponseEntity<List<Book>> searchBooks(@RequestParam(name = "q") String query) {
        List<Book> results = bookService.searchBooks(query);
        return ResponseEntity.ok(results);
    }

    // POST /api/v1/books - Create a new book
    @PostMapping
    public ResponseEntity<Book> createBook(@RequestBody Book book) {
        Book savedBook = bookService.saveBook(book);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(savedBook.getId())
                .toUri();

        return ResponseEntity.created(location).body(savedBook);
    }

    // PUT /api/v1/books/{id} - Full update (replace entire resource)
    @PutMapping("/{id}")
    public ResponseEntity<Book> updateBook(@PathVariable Long id, @RequestBody Book book) {
        Book existingBook = bookService.findBookById(id);
        if (existingBook == null) {
            return ResponseEntity.notFound().build();
        }

        book.setId(id);
        Book updatedBook = bookService.saveBook(book);

        return ResponseEntity.ok(updatedBook);
    }

    // PATCH /api/v1/books/{id} - Partial update
    @PatchMapping("/{id}")
    public ResponseEntity<Book> partialUpdateBook(@PathVariable Long id, @RequestBody Book bookUpdates) {
        Book existingBook = bookService.findBookById(id);
        if (existingBook == null) {
            return ResponseEntity.notFound().build();
        }

        // Update only provided fields
        if (bookUpdates.getTitle() != null) {
            existingBook.setTitle(bookUpdates.getTitle());
        }
        if (bookUpdates.getAuthor() != null) {
            existingBook.setAuthor(bookUpdates.getAuthor());
        }
        if (bookUpdates.getCoverUrl() != null) {
            existingBook.setCoverUrl(bookUpdates.getCoverUrl());
        }

        Book updatedBook = bookService.saveBook(existingBook);
        return ResponseEntity.ok(updatedBook);
    }

    // DELETE /api/v1/books/{id} - Delete a book
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        Book existingBook = bookService.findBookById(id);
        if (existingBook == null) {
            return ResponseEntity.notFound().build();
        }

        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }
}