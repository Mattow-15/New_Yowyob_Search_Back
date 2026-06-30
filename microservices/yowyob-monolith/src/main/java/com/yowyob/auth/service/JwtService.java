/**
 * Service for generating and validating JSON Web Tokens (JWT).
 * Handles access token and refresh token creation and validation.
 * @author Matteo Owona, Rouchda Yampen
 * @date 2024-01-14
 * @updated 2025-02-11
 */
package com.yowyob.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    @Value("${jwt.refresh-expiration}")
    private Long refreshExpiration;

    /**
     * Returns the signing key derived from the secret.
     *
     * @return the HMAC signing key
     */
    private SecretKey getSigningKey() {
        byte[] key_bytes = secret.getBytes();
        return Keys.hmacShaKeyFor(key_bytes);
    }

    /**
     * Generates a JWT access token for the given user.
     *
     * @param user_id the user's unique identifier
     * @param email   the user's email address
     * @param role    the user's role
     * @return the signed JWT access token string
     */
    public String generateAccessToken(UUID user_id, String email, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", email);
        claims.put("role", role);

        return Jwts.builder()
                .claims(claims)
                .subject(user_id.toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Generates a JWT refresh token for the given user.
     *
     * @param user_id the user's unique identifier
     * @return the signed JWT refresh token string
     */
    public String generateRefreshToken(UUID user_id) {
        return Jwts.builder()
                .subject(user_id.toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extracts the user ID (subject) from a JWT token.
     *
     * @param token the JWT token string
     * @return the user ID stored in the token
     */
    public String extractUserId(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Extracts the email claim from a JWT token.
     *
     * @param token the JWT token string
     * @return the email stored in the token
     */
    public String extractEmail(String token) {
        return extractAllClaims(token).get("email", String.class);
    }

    /**
     * Validates whether a JWT token is still valid (not expired).
     *
     * @param token the JWT token string
     * @return true if the token is valid, false otherwise
     */
    public boolean isTokenValid(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks if a JWT token has expired.
     *
     * @param token the JWT token string
     * @return true if the token is expired
     */
    private boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    /**
     * Extracts all claims from a JWT token.
     *
     * @param token the JWT token string
     * @return the claims payload
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Returns the configured token expiration time in milliseconds.
     *
     * @return the expiration time
     */
    public Long getExpirationTime() {
        return expiration;
    }
}
