package com.booktracker.bookservice.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.booktracker.bookservice.entity.Book;
import com.booktracker.bookservice.repository.BookRepository;

@Service
public class BookService {

    @Autowired
    private BookRepository bookRepository;

    // Get all books
    public List<Book> findAllBooks() {
        return bookRepository.findAll();
    }

    // Get book by ID
    public Book findBookById(Long id) {
        return bookRepository.findById(id).orElse(null);
    }

    // Search books by title or author
    public List<Book> searchBooks(String query) {
        String searchQuery = query.toLowerCase();
        return bookRepository.findAll().stream()
                .filter(book ->
                        book.getTitle().toLowerCase().contains(searchQuery) ||
                                (book.getAuthor() != null && book.getAuthor().toLowerCase().contains(searchQuery))
                )
                .collect(Collectors.toList());
    }

    // Save/Create book
    public Book saveBook(Book book) {
        return bookRepository.save(book);
    }

    // Update book
    public Book updateBook(Long id, Book bookDetails) {
        Book book = findBookById(id);
        if (book != null) {
            book.setTitle(bookDetails.getTitle());
            book.setAuthor(bookDetails.getAuthor());
            book.setCoverUrl(bookDetails.getCoverUrl());
            return bookRepository.save(book);
        }
        return null;
    }

    // Delete book
    public void deleteBook(Long id) {
        bookRepository.deleteById(id);
    }

    // Check if book exists
    public boolean bookExists(Long id) {
        return bookRepository.existsById(id);
    }
}