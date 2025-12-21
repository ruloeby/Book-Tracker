package com.booktracker.bookservice.controller;

import com.booktracker.bookservice.entity.User;
import com.booktracker.bookservice.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@CrossOrigin(origins = "http://localhost:8080")
public class UserRestController {

    private final UserService userService;

    // Use constructor injection
    public UserRestController(UserService userService) {
        this.userService = userService;
    }

    // --- 1. USER REGISTRATION (Data Sync) ---
    // This assumes the Vitrine calls this endpoint after a user registers successfully in Keycloak.
    // NOTE: This endpoint should not save the clear password, as Keycloak manages it.
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody User user) {
        try {
            // CRITICAL: Ensure the password field is set to a dummy value or null
            // since the real password is managed by Keycloak.
            user.setPassword("KEYCLOAK_MANAGED");

            userService.save(user); // Use save, as register implies hashing

            Map<String, String> response = new HashMap<>();
            response.put("message", "User data synchronized successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error synchronizing user data: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // --- 2. USER LOGIN (REMOVED) ---
    // The login flow is now: Client -> Keycloak -> Client receives JWT -> Client sends JWT to Book Service.
    // The Book Service should NOT have a /login endpoint.

    // --- 3. PROTECTED ENDPOINT - Get User Info from Token ---
    @GetMapping("/me")
    // Use @AuthenticationPrincipal Jwt to extract claims directly from the validated token
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {

        // The JWT is guaranteed to be valid by the SecurityConfig
        String email = jwt.getClaimAsString("email"); // Assuming Keycloak puts the email in the 'email' claim

        // Look up the synchronized user data in the local database
        User user = userService.findByEmail(email);

        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("error", "User found in token but not in local database."));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("email", user.getEmail());
        response.put("name", user.getName() != null ? user.getName() : email.split("@")[0]);
        // Do NOT return the dummy password

        return ResponseEntity.ok(response);
    }

    // GET /api/v1/users/{id} - Protected, but can be used internally or by admin
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return userService.findById(id)
                .map(user -> {
                    // Do not return the dummy password
                    user.setPassword(null);
                    return ResponseEntity.ok(user);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/sync")
    public ResponseEntity<?> syncUser(@RequestBody Map<String, String> request) {
        try {
            String keycloakId = request.get("keycloakId");
            String email = request.get("email");
            String name = request.get("name");

            User user = userService.findOrCreateByKeycloak(keycloakId, email, name);

            Map<String, Object> response = new HashMap<>();
            response.put("id", user.getId());
            response.put("email", user.getEmail());
            response.put("name", user.getName());
            response.put("keycloakId", user.getKeycloakId());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}