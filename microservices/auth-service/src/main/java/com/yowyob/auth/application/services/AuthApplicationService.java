package com.yowyob.auth.application.services;

import com.yowyob.auth.application.ports.in.AuthenticateUseCase;
import com.yowyob.auth.application.ports.in.RegisterUseCase;
import com.yowyob.auth.application.ports.out.*;
import com.yowyob.auth.domain.model.AuthResult;
import com.yowyob.auth.domain.model.GeoLocation;
import com.yowyob.auth.domain.model.GoogleUserInfo;
import com.yowyob.auth.domain.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class AuthApplicationService implements RegisterUseCase, AuthenticateUseCase {

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordEncoderPort passwordEncoderPort;
    private final TokenServicePort tokenServicePort;
    private final IdentityProviderPort identityProviderPort;
    private final UserEventPublisherPort userEventPublisherPort;
    private final GeoLocatorPort geoLocatorPort;

    @Override
    public AuthResult register(String name, String email, String password, String ipAddress) {
        String normalizedEmail = email.trim().toLowerCase();
        log.info("Starting registration process for email: {}", normalizedEmail);

        if (userRepositoryPort.existsByEmail(normalizedEmail)) {
            log.warn("Registration failed: Email already exists: {}", normalizedEmail);
            throw new RuntimeException("Email already exists");
        }

        User user = User.builder()
                .name(name)
                .email(normalizedEmail)
                .password(passwordEncoderPort.encode(password))
                .role(User.Role.USER)
                .status(User.Status.ACTIVE)
                .emailVerified(false)
                .build();

        user = userRepositoryPort.save(user);
        log.info("User saved with ID: {}", user.getId());

        try {
            userEventPublisherPort.publishUserCreated(user);
        } catch (Exception e) {
            log.error("Failed to send Kafka event for user {}: {}", user.getEmail(), e.getMessage());
        }

        return generateAuthResult(user, "User registered successfully", ipAddress);
    }

    @Override
    public AuthResult login(String email, String password, String ipAddress) {
        String normalizedEmail = email.trim().toLowerCase();
        log.info("Login request received for email: {}", normalizedEmail);

        User user = userRepositoryPort.findByEmail(normalizedEmail)
                .orElseThrow(() -> {
                    log.warn("Login failed: User not found with email: {}", normalizedEmail);
                    return new RuntimeException("Invalid credentials");
                });

        if (!passwordEncoderPort.matches(password, user.getPassword())) {
            log.warn("Login failed: Password mismatch for email: {}", normalizedEmail);
            throw new RuntimeException("Invalid credentials");
        }

        if (user.getStatus() != User.Status.ACTIVE) {
            throw new RuntimeException("Account is not active");
        }

        return generateAuthResult(user, "Login successful", ipAddress);
    }

    @Override
    public AuthResult googleLogin(String googleToken, String ipAddress) {
        try {
            GoogleUserInfo userInfo = identityProviderPort.verifyGoogleToken(googleToken);
            String email = userInfo.getEmail().trim().toLowerCase();
            String name = userInfo.getName();

            log.info("Google Login for email: {}", email);

            Optional<User> userOptional = userRepositoryPort.findByEmail(email);
            User user;
            boolean isNewUser = false;

            if (userOptional.isPresent()) {
                user = userOptional.get();
                log.info("Existing user found for Google email: {}", email);
            } else {
                isNewUser = true;
                log.info("New User from Google ({}). Registering...", email);
                user = User.builder()
                        .name(name != null ? name : email)
                        .email(email)
                        .password(passwordEncoderPort.encode(UUID.randomUUID().toString()))
                        .role(User.Role.USER)
                        .status(User.Status.ACTIVE)
                        .emailVerified(true)
                        .build();
                user = userRepositoryPort.save(user);
                log.info("User auto-registered via Google: {}", user.getEmail());
            }

            if (user.getStatus() != User.Status.ACTIVE) {
                throw new RuntimeException("Account is not active");
            }

            if (isNewUser) {
                try {
                    userEventPublisherPort.publishUserCreated(user);
                    log.info("Kafka event sent for Google user: {}", user.getEmail());
                } catch (Exception e) {
                    log.error("Failed to send Kafka event for Google user {}: {}", user.getEmail(), e.getMessage());
                }
            }

            return generateAuthResult(user, "Google Login successful", ipAddress);

        } catch (Exception e) {
            log.error("Google Login failed", e);
            throw new RuntimeException("Google Login failed: " + e.getMessage());
        }
    }

    @Override
    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        User user = userRepositoryPort.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoderPort.matches(currentPassword, user.getPassword())) {
            throw new RuntimeException("Invalid current password");
        }

        User updatedUser = user.toBuilder()
                .password(passwordEncoderPort.encode(newPassword))
                .build();
        userRepositoryPort.save(updatedUser);
        log.info("Password changed successfully for user: {}", user.getEmail());
    }

    private AuthResult generateAuthResult(User user, String message, String ipAddress) {
        String accessToken = tokenServicePort.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name());

        String refreshToken = tokenServicePort.generateRefreshToken(user.getId());

        GeoLocation location = resolveLocation(ipAddress);

        return AuthResult.builder()
                .user(user)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(tokenServicePort.getExpirationTime())
                .message(message)
                .location(location)
                .build();
    }

    /**
     * Géolocalise l'IP de l'utilisateur. Best-effort : un échec ne doit jamais
     * empêcher la connexion, on renvoie simplement {@code null}.
     */
    private GeoLocation resolveLocation(String ipAddress) {
        try {
            return geoLocatorPort.locate(ipAddress).orElse(null);
        } catch (Exception e) {
            log.warn("Géolocalisation impossible pour l'IP {}: {}", ipAddress, e.getMessage());
            return null;
        }
    }
}
