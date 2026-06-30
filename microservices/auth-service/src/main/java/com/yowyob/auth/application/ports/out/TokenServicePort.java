package com.yowyob.auth.application.ports.out;

import java.util.UUID;

public interface TokenServicePort {
    String generateAccessToken(UUID userId, String email, String role);
    String generateRefreshToken(UUID userId);
    Long getExpirationTime();
}
