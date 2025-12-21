package com.booktracker.bookservice.service;

import com.booktracker.bookservice.entity.User;
import com.booktracker.bookservice.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    // We no longer inject PasswordEncoder here, as password hashing should be handled
    // by the client (Vitrine) for storage or by Keycloak for auth validation.
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User save(User user) {
        // NOTE: If this service handles registration, the User object passed here must
        // have its password field already encoded (e.g., encoded in AuthRestController
        // before calling save). However, since we are moving to Keycloak, this password
        // logic should ideally be removed entirely from the REST service's registration flow.
        return userRepository.save(user);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User findByKeycloakId(String keycloakId) {
        return userRepository.findByKeycloakId(keycloakId);
    }

    public User findOrCreateByKeycloak(String keycloakId, String email, String name) {
        // First, try to find by keycloakId
        User user = userRepository.findByKeycloakId(keycloakId);

        if (user != null) {
            return user;
        }

        // If not found by keycloakId, try to find by email (for existing users)
        user = userRepository.findByEmail(email);

        if (user != null) {
            // Existing user found - update their keycloakId to link accounts
            user.setKeycloakId(keycloakId);
            if (user.getName() == null || user.getName().isEmpty()) {
                user.setName(name);
            }
            return userRepository.save(user);
        }

        // No existing user found - create new one
        user = new User();
        user.setKeycloakId(keycloakId);
        user.setEmail(email);
        user.setName(name);
        user.setPassword("KEYCLOAK_MANAGED");
        return userRepository.save(user);
    }
}