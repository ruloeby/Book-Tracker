package com.booktracker.vitrine.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class AuthController {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${keycloak.token.url}")
    private String keycloakTokenUrl;

    @Value("${keycloak.client.id}")
    private String clientId;

    @Value("${keycloak.client.secret}")
    private String clientSecret;

    @Value("${keycloak.admin.url:http://localhost:8180}")
    private String keycloakUrl;

    @Value("${keycloak.realm:booktracker}")
    private String realm;

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String login(
            @RequestParam String email,
            @RequestParam String password,
            HttpServletResponse response,
            Model model
    ) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("client_id", clientId);
            map.add("client_secret", clientSecret);
            map.add("username", email);
            map.add("password", password);
            map.add("grant_type", "password");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

            System.out.println(" Attempting login for: " + email);
            System.out.println(" Token URL: " + keycloakTokenUrl);

            ResponseEntity<Map> keycloakResponse = restTemplate.postForEntity(
                    keycloakTokenUrl, request, Map.class);

            if (keycloakResponse.getStatusCode().is2xxSuccessful() && keycloakResponse.getBody() != null) {
                String accessToken = (String) keycloakResponse.getBody().get("access_token");

                if (accessToken == null || accessToken.isEmpty()) {
                    System.err.println(" No access token in response");
                    model.addAttribute("error", "Failed to get access token");
                    return "login";
                }

                System.out.println(" Login successful for: " + email);

                Cookie cookie = new Cookie("AUTH-TOKEN", accessToken);
                cookie.setHttpOnly(true);
                cookie.setPath("/");
                cookie.setMaxAge(30 * 60);
                response.addCookie(cookie);

                return "redirect:/dashboard";
            } else {
                System.err.println(" Keycloak returned: " + keycloakResponse.getStatusCode());
                model.addAttribute("error", "Invalid credentials");
                return "login";
            }
        } catch (Exception e) {
            System.err.println(" Login error: " + e.getMessage());
            e.printStackTrace();

            String errorMessage = "Login failed";
            if (e.getMessage() != null) {
                if (e.getMessage().contains("401")) {
                    errorMessage = "Invalid email or password. Please check your credentials.";
                } else if (e.getMessage().contains("Connection refused")) {
                    errorMessage = "Cannot connect to authentication server. Please try again later.";
                } else if (e.getMessage().contains("timeout")) {
                    errorMessage = "Request timed out. Please try again.";
                }
            }

            model.addAttribute("error", errorMessage);
            return "login";
        }
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/register")
    public String register(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            Model model
    ) {
        // Validate passwords match
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match");
            return "register";
        }

        if (password.length() < 6) {
            model.addAttribute("error", "Password must be at least 6 characters");
            return "register";
        }

        try {
            // Get admin token
            String adminToken = getKeycloakAdminToken();

            if (adminToken == null) {
                model.addAttribute("error", "Registration service unavailable");
                return "register";
            }

            // Create user in Keycloak
            boolean created = createKeycloakUser(adminToken, name, email, password);

            if (created) {
                return "redirect:/login?success";
            } else {
                model.addAttribute("error", "Email might already be registered");
                return "register";
            }
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Registration failed: " + e.getMessage());
            return "register";
        }
    }

    private String getKeycloakAdminToken() {
        try {
            String tokenUrl = keycloakUrl + "/realms/master/protocol/openid-connect/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            // Use admin credentials (you need to configure these)
            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("client_id", "admin-cli");
            map.add("username", "admin");  // Your Keycloak admin username
            map.add("password", "admin");  // Your Keycloak admin password
            map.add("grant_type", "password");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return (String) response.getBody().get("access_token");
            }
        } catch (Exception e) {
            System.err.println("Failed to get admin token: " + e.getMessage());
        }
        return null;
    }

    private boolean createKeycloakUser(String adminToken, String name, String email, String password) {
        try {
            String usersUrl = keycloakUrl + "/admin/realms/" + realm + "/users";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(adminToken);

            // Create user with credentials included and NO required actions
            Map<String, Object> user = new HashMap<>();
            user.put("username", email);
            user.put("email", email);
            user.put("firstName", name);
            user.put("lastName", name);
            user.put("enabled", true);
            user.put("emailVerified", true);
            user.put("requiredActions", List.of());  // Empty list - no required actions

            // Include credentials directly in user creation
            Map<String, Object> credential = new HashMap<>();
            credential.put("type", "password");
            credential.put("value", password);
            credential.put("temporary", false);
            user.put("credentials", List.of(credential));

            HttpEntity<Map<String, Object>> createRequest = new HttpEntity<>(user, headers);
            ResponseEntity<String> createResponse = restTemplate.postForEntity(usersUrl, createRequest, String.class);

            return createResponse.getStatusCode() == HttpStatus.CREATED;
        } catch (Exception e) {
            System.err.println("Failed to create user: " + e.getMessage());
            return false;
        }
    }



    @GetMapping("/logout")
    public String logout(HttpServletResponse response) {
        // Clear the authentication cookie
        Cookie cookie = new Cookie("AUTH-TOKEN", null);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);
        response.addCookie(cookie);

        // Redirect to home page
        return "redirect:/home";
    }
}