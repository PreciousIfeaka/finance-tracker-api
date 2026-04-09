package com.precious.finance_tracker.configurations;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        // Minimum 256-bit secret key for HMAC-SHA256 Base64URL encoded
        String testSecretKey = "4xS7a4XzYxJ4Qj7N5gV3qE2wH5yL8vB6xZ1cM7pK9tU=";
        ReflectionTestUtils.setField(jwtService, "secretKey", testSecretKey);

        long testJwtExpiry = 3600000;
        ReflectionTestUtils.setField(jwtService, "jwtExpiry", testJwtExpiry);
    }

    @Test
    void generateToken_ShouldReturnValidJwtString() {
        String token = jwtService.generateToken("user@test.com");

        assertNotNull(token);
        assertEquals(3, token.split("\\.").length); // Basic structure check
    }

    @Test
    void extractUsername_ShouldReturnCorrectSubject() {
        String token = jwtService.generateToken("user@test.com");
        String username = jwtService.extractUsername(token);

        assertEquals("user@test.com", username);
    }

    @Test
    void extractClaims_ShouldReturnAllClaims() {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", "ADMIN");

        String token = jwtService.generateToken("admin@test.com", extraClaims);
        Claims claims = jwtService.extractClaims(token);

        assertEquals("admin@test.com", claims.getSubject());
        assertEquals("ADMIN", claims.get("role"));
        assertNotNull(claims.getExpiration());
        assertNotNull(claims.getIssuedAt());
    }

    @Test
    void isTokenValid_ShouldReturnTrue_WhenTokenMatchesUserAndNotExpired() {
        String username = "user@test.com";
        String token = jwtService.generateToken(username);

        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn(username);

        assertTrue(jwtService.isTokenValid(token, userDetails));
    }

    @Test
    void isTokenValid_ShouldReturnFalse_WhenUsernameMismatch() {
        String token = jwtService.generateToken("user@test.com");

        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("other@test.com");

        assertFalse(jwtService.isTokenValid(token, userDetails));
    }
}
