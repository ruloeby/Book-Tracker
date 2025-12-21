package com.booktracker.bookservice.repository;





import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.booktracker.bookservice.entity.Book;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

}