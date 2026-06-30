/**
 * Authentication Controller for user registration and login.
 * @author Matteo Owona, Rouchda Yampen
 * @date 2024-01-14
 */
package com.yowyob.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.yowyob.auth.dto.AuthResponse;
import com.yowyob.auth.dto.LoginRequest;
import com.yowyob.auth.dto.RegisterRequest;
import com.yowyob.auth.dto.GoogleLoginRequest;
import com.yowyob.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Endpoints for user registration and login")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a new user account with the provided details")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration request received for email: {}", request.getEmail());
        AuthResponse response = authService.register(request);
        log.info("Registration successful for email: {}", request.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Login user", description = "Authenticates a user and returns JWT tokens")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/google")
    @Operation(summary = "Google Login", description = "Authenticates user via Google ID Token")
    public ResponseEntity<AuthResponse> googleLogin(@RequestBody GoogleLoginRequest request) {
        return ResponseEntity.ok(authService.googleLogin(request));
    }

    @org.springframework.web.bind.annotation.PutMapping("/change-password")
    @Operation(summary = "Change Password", description = "Allows authenticated user to change their password")
    public ResponseEntity<String> changePassword(
            @org.springframework.web.bind.annotation.RequestHeader(value = "X-User-Id", required = false) String userId,
            @Valid @RequestBody com.yowyob.auth.dto.ChangePasswordRequest request) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User account ID is missing in header.");
        }
        authService.changePassword(java.util.UUID.fromString(userId), request);
        return ResponseEntity.ok("Password changed successfully.");
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Auth Service is running!");
    }

}
