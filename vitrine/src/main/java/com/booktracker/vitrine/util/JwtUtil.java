package com.booktracker.vitrine.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.Map;

@Component
public class JwtUtil {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Map<String, Object> extractClaims(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            return objectMapper.readValue(payload, Map.class);
        } catch (Exception e) {
            return null;
        }
    }

    public String extractUsername(String token) {
        Map<String, Object> claims = extractClaims(token);
        if (claims == null) return null;
        return (String) claims.getOrDefault("preferred_username", claims.get("sub"));
    }

    public String extractSubject(String token) {
        Map<String, Object> claims = extractClaims(token);
        if (claims == null) return null;
        return (String) claims.get("sub");
    }

    public Long extractUserId(String token) {
        String sub = extractSubject(token);
        if (sub != null) {
            return (long) sub.hashCode();
        }
        return null;
    }

    public String extractEmail(String token) {
        Map<String, Object> claims = extractClaims(token);
        if (claims == null) return null;
        return (String) claims.get("email");
    }

    public boolean validateToken(String token) {
        try {
            Map<String, Object> claims = extractClaims(token);
            if (claims == null) return false;

            Object expObj = claims.get("exp");
            if (expObj != null) {
                long exp;
                if (expObj instanceof Integer) {
                    exp = ((Integer) expObj).longValue();
                } else {
                    exp = (Long) expObj;
                }
                return exp * 1000 > System.currentTimeMillis();
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}