package com.booktracker.bookservice.repository;




import com.booktracker.bookservice.entity.ReadingProgress;
import com.booktracker.bookservice.entity.UserLibrary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReadingProgressRepository extends JpaRepository<ReadingProgress, Long> {

    // Trouver la progression par UserLibrary
    Optional<ReadingProgress> findByUserLibrary(UserLibrary userLibrary);

    // Trouver par UserLibrary ID
    Optional<ReadingProgress> findByUserLibraryId(Long userLibraryId);
}