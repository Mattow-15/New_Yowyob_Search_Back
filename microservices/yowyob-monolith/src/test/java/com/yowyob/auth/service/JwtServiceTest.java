/**
 * Unit tests for JwtService.
 * Tests JWT token generation, validation, and claims extraction.
 * @author Matteo Owona, Rouchda Yampen
 * @date 2025-02-11
 */
package com.yowyob.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private JwtService jwt_service;
    private static final String TEST_SECRET = "ThisIsAVeryLongSecretKeyForTestingPurposes12345678901234567890";
    private static final Long TEST_EXPIRATION = 3600000L; // 1 hour
    private static final Long TEST_REFRESH_EXPIRATION = 86400000L; // 24 hours

    @BeforeEach
    void setUp() {
        jwt_service = new JwtService();
        ReflectionTestUtils.setField(jwt_service, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwt_service, "expiration", TEST_EXPIRATION);
        ReflectionTestUtils.setField(jwt_service, "refreshExpiration", TEST_REFRESH_EXPIRATION);
    }

    // ===== Token Generation tests =====

    @Test
    @DisplayName("generateAccessToken - should generate a non-null token")
    void generateAccessToken_returns_non_null() {
        UUID user_id = UUID.randomUUID();
        String token = jwt_service.generateAccessToken(user_id, "test@test.com", "USER");
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    @DisplayName("generateAccessToken - should produce different tokens for different users")
    void generateAccessToken_unique_per_user() {
        UUID user_id_1 = UUID.randomUUID();
        UUID user_id_2 = UUID.randomUUID();
        String token_1 = jwt_service.generateAccessToken(user_id_1, "user1@test.com", "USER");
        String token_2 = jwt_service.generateAccessToken(user_id_2, "user2@test.com", "USER");
        assertFalse(token_1.equals(token_2));
    }

    @Test
    @DisplayName("generateRefreshToken - should generate a non-null token")
    void generateRefreshToken_returns_non_null() {
        UUID user_id = UUID.randomUUID();
        String token = jwt_service.generateRefreshToken(user_id);
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    // ===== Claims Extraction tests =====

    @Test
    @DisplayName("extractUserId - should return the correct user ID")
    void extractUserId_returns_correct_id() {
        UUID user_id = UUID.randomUUID();
        String token = jwt_service.generateAccessToken(user_id, "test@test.com", "USER");
        String extracted_id = jwt_service.extractUserId(token);
        assertEquals(user_id.toString(), extracted_id);
    }

    @Test
    @DisplayName("extractEmail - should return the correct email")
    void extractEmail_returns_correct_email() {
        UUID user_id = UUID.randomUUID();
        String email = "test@example.com";
        String token = jwt_service.generateAccessToken(user_id, email, "USER");
        String extracted_email = jwt_service.extractEmail(token);
        assertEquals(email, extracted_email);
    }

    // ===== Token Validation tests =====

    @Test
    @DisplayName("isTokenValid - should return true for a valid token")
    void isTokenValid_valid_token() {
        UUID user_id = UUID.randomUUID();
        String token = jwt_service.generateAccessToken(user_id, "test@test.com", "USER");
        assertTrue(jwt_service.isTokenValid(token));
    }

    @Test
    @DisplayName("isTokenValid - should return false for an invalid token")
    void isTokenValid_invalid_token() {
        assertFalse(jwt_service.isTokenValid("invalid.token.string"));
    }

    @Test
    @DisplayName("isTokenValid - should return false for an expired token")
    void isTokenValid_expired_token() {
        // Set expiration to -1 (already expired)
        ReflectionTestUtils.setField(jwt_service, "expiration", -1L);
        UUID user_id = UUID.randomUUID();
        String token = jwt_service.generateAccessToken(user_id, "test@test.com", "USER");
        assertFalse(jwt_service.isTokenValid(token));
    }

    // ===== Expiration Time test =====

    @Test
    @DisplayName("getExpirationTime - should return configured value")
    void getExpirationTime_returns_configured() {
        assertEquals(TEST_EXPIRATION, jwt_service.getExpirationTime());
    }

    @Test
    @DisplayName("generateRefreshToken - extractUserId should work on refresh token")
    void refreshToken_extractUserId() {
        UUID user_id = UUID.randomUUID();
        String token = jwt_service.generateRefreshToken(user_id);
        String extracted_id = jwt_service.extractUserId(token);
        assertEquals(user_id.toString(), extracted_id);
    }
}
