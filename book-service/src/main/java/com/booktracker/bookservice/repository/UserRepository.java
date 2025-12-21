package com.booktracker.bookservice.repository;



import org.springframework.data.jpa.repository.JpaRepository;

import com.booktracker.bookservice.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);
    User findByKeycloakId(String keycloakId);
}
