/**
 * Authentication service handling user registration, login, and Google OAuth.
 * Manages user credential validation and JWT token generation.
 * @author Matteo Owona, Rouchda Yampen
 * @date 2024-01-14
 * @updated 2025-02-11
 */
package com.yowyob.auth.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.yowyob.auth.dto.AuthResponse;
import com.yowyob.auth.dto.GoogleLoginRequest;
import com.yowyob.auth.dto.LoginRequest;
import com.yowyob.auth.dto.RegisterRequest;
import com.yowyob.auth.entity.User;
import com.yowyob.auth.exception.AccountNotActiveException;
import com.yowyob.auth.exception.EmailAlreadyExistsException;
import com.yowyob.auth.exception.GoogleLoginException;
import com.yowyob.auth.exception.InvalidCredentialsException;
import com.yowyob.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

        private final UserRepository userRepository;
        private final PasswordEncoder passwordEncoder;
        private final JwtService jwtService;
        private final GoogleAuthVerifier googleAuthVerifier;
        private final KafkaProducerService kafkaProducerService;

        /**
         * Registers a new user with the provided details.
         *
         * @param request the registration request containing user info
         * @return authentication response with JWT tokens
         * @throws EmailAlreadyExistsException if the email is already registered
         */
        @Transactional("authTransactionManager")
        public AuthResponse register(RegisterRequest request) {
                String email = request.getEmail().trim().toLowerCase();
                log.info("Starting registration process for email: {}", email);
                if (userRepository.existsByEmail(email)) {
                        log.warn("Registration failed: Email already exists: {}", email);
                        throw new EmailAlreadyExistsException(email);
                }

                User user = User.builder()
                                .name(request.getName())
                                .email(email)
                                .password(passwordEncoder.encode(request.getPassword()))
                                .role(User.Role.USER)
                                .status(User.Status.ACTIVE)
                                .emailVerified(false)
                                .build();

                user = userRepository.save(user);
                log.info("User saved with ID: {}", user.getId());

                try {
                        com.yowyob.auth.event.UserCreatedEvent event = com.yowyob.auth.event.UserCreatedEvent.builder()
                                        .id(user.getId().toString())
                                        .email(user.getEmail())
                                        .username(user.getName())
                                        .firstName(user.getName())
                                        .build();
                        kafkaProducerService.sendUserCreatedEvent(event);
                } catch (Exception e) {
                        log.error("Failed to send event for user {}: {}", user.getEmail(), e.getMessage());
                }

                return generateAuthResponse(user, "User registered successfully");
        }

        /**
         * Authenticates a user with email and password.
         *
         * @param request the login request containing credentials
         * @return authentication response with JWT tokens
         * @throws InvalidCredentialsException if credentials are invalid
         * @throws AccountNotActiveException   if the account is not active
         */
        public AuthResponse login(LoginRequest request) {
                String email = request.getEmail().trim().toLowerCase();
                log.info("Login request received for email: {}", email);
                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> {
                                        log.warn("Login failed: User not found with email: {}", email);
                                        return new InvalidCredentialsException();
                                });

                if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                        log.warn("Login failed: Password mismatch for email: {}", email);
                        throw new InvalidCredentialsException();
                }

                if (user.getStatus() != User.Status.ACTIVE) {
                        throw new AccountNotActiveException(email);
                }

                return generateAuthResponse(user, "Login successful");
        }

        /**
         * Authenticates a user via Google OAuth ID Token.
         * Creates a new account if the user does not exist.
         *
         * @param request the Google login request containing the ID token
         * @return authentication response with JWT tokens
         * @throws GoogleLoginException      if Google authentication fails
         * @throws AccountNotActiveException if the account is not active
         */
        @Transactional("authTransactionManager")
        public AuthResponse googleLogin(GoogleLoginRequest request) {
                try {
                        GoogleIdToken.Payload payload = googleAuthVerifier.verify(request.getToken());
                        String email = payload.getEmail().trim().toLowerCase();
                        String name = (String) payload.get("name");

                        log.info("Google Login for email: {}", email);

                        Optional<User> user_optional = userRepository.findByEmail(email);
                        User user;

                        if (user_optional.isPresent()) {
                                user = user_optional.get();
                                log.info("Existing user found for Google email: {}", email);
                        } else {
                                log.info("New User from Google ({}). Registering...", email);
                                user = User.builder()
                                                .name(name != null ? name : email)
                                                .email(email)
                                                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                                                .role(User.Role.USER)
                                                .status(User.Status.ACTIVE)
                                                .emailVerified(true)
                                                .build();
                                user = userRepository.save(user);
                                log.info("User auto-registered via Google: {}", user.getEmail());

                                try {
                                        com.yowyob.auth.event.UserCreatedEvent event = com.yowyob.auth.event.UserCreatedEvent
                                                        .builder()
                                                        .id(user.getId().toString())
                                                        .email(user.getEmail())
                                                        .username(user.getName())
                                                        .firstName(user.getName())
                                                        .build();
                                        kafkaProducerService.sendUserCreatedEvent(event);
                                        log.info("Event sent for Google user: {}", user.getEmail());
                                } catch (Exception e) {
                                        log.error("Failed to send event for Google user {}: {}", user.getEmail(),
                                                        e.getMessage());
                                }
                        }

                        if (user.getStatus() != User.Status.ACTIVE) {
                                throw new AccountNotActiveException(email);
                        }

                        return generateAuthResponse(user, "Google Login successful");

                } catch (AccountNotActiveException e) {
                        throw e;
                } catch (Exception e) {
                        log.error("Google Login failed", e);
                        throw new GoogleLoginException(e.getMessage(), e);
                }
        }

        /**
         * Changes the password of an existing user account.
         *
         * @param userId  The ID of the user requesting password change
         * @param request the password change request with current and new passwords
         */
        @Transactional("authTransactionManager")
        public void changePassword(UUID userId, com.yowyob.auth.dto.ChangePasswordRequest request) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new com.yowyob.auth.exception.ResourceNotFoundException("User not found"));

                // Validate current password
                if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                        throw new InvalidCredentialsException("Current password is incorrect");
                }

                // Update with new password
                user.setPassword(passwordEncoder.encode(request.getNewPassword()));
                userRepository.save(user);
                log.info("Password successfully changed for user ID: {}", userId);
        }

        /**
         * Generates an authentication response with JWT access and refresh tokens.
         *
         * @param user    the authenticated user entity
         * @param message the success message to include in the response
         * @return the authentication response with tokens and user info
         */
        private AuthResponse generateAuthResponse(User user, String message) {
                String access_token = jwtService.generateAccessToken(
                                user.getId(),
                                user.getEmail(),
                                user.getRole().name());

                String refresh_token = jwtService.generateRefreshToken(user.getId());

                return AuthResponse.builder()
                                .success(true)
                                .message(message)
                                .accessToken(access_token)
                                .refreshToken(refresh_token)
                                .expiresIn(jwtService.getExpirationTime())
                                .user(AuthResponse.UserDto.builder()
                                                .id(user.getId().toString())
                                                .name(user.getName())
                                                .email(user.getEmail())
                                                .role(user.getRole().name())
                                                .build())
                                .build();
        }
}
