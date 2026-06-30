package com.yowyob.auth.infrastructure.adapters.in.web;

import com.yowyob.auth.application.ports.in.AuthenticateUseCase;
import com.yowyob.auth.application.ports.in.RegisterUseCase;
import com.yowyob.auth.domain.model.AuthResult;
import com.yowyob.auth.domain.model.GeoLocation;
import com.yowyob.auth.infrastructure.adapters.in.web.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Endpoints for user registration and login")
public class AuthController {

    private final RegisterUseCase registerUseCase;
    private final AuthenticateUseCase authenticateUseCase;

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a new user account with the provided details")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request,
                                                 HttpServletRequest httpRequest) {
        log.info("Registration request received for email: {}", request.getEmail());
        String clientIp = extractClientIp(httpRequest);
        AuthResult result = registerUseCase.register(request.getName(), request.getEmail(), request.getPassword(), clientIp);
        log.info("Registration successful for email: {}", request.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(result));
    }

    @PostMapping("/login")
    @Operation(summary = "Login user", description = "Authenticates a user and returns JWT tokens")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletRequest httpRequest) {
        String clientIp = extractClientIp(httpRequest);
        log.info("Login request received for email: {} from IP: {}", request.getEmail(), clientIp);
        AuthResult result = authenticateUseCase.login(request.getEmail(), request.getPassword(), clientIp);
        return ResponseEntity.ok(toResponse(result));
    }

    @PostMapping("/google")
    @Operation(summary = "Google Login", description = "Authenticates user via Google ID Token")
    public ResponseEntity<AuthResponse> googleLogin(@RequestBody GoogleLoginRequest request,
                                                    HttpServletRequest httpRequest) {
        String clientIp = extractClientIp(httpRequest);
        AuthResult result = authenticateUseCase.googleLogin(request.getToken(), clientIp);
        return ResponseEntity.ok(toResponse(result));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Auth Service is running!");
    }

    @PutMapping("/change-password")
    @Operation(summary = "Change Password", description = "Changes the user's password")
    public ResponseEntity<Void> changePassword(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Valid @RequestBody ChangePasswordRequest request) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        authenticateUseCase.changePassword(UUID.fromString(userId), request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.ok().build();
    }

    private AuthResponse toResponse(AuthResult result) {
        if (result == null) return null;
        return AuthResponse.builder()
                .success(true)
                .message(result.getMessage())
                .accessToken(result.getAccessToken())
                .refreshToken(result.getRefreshToken())
                .expiresIn(result.getExpiresIn())
                .user(AuthResponse.UserDto.builder()
                        .id(result.getUser().getId().toString())
                        .name(result.getUser().getName())
                        .email(result.getUser().getEmail())
                        .role(result.getUser().getRole().name())
                        .build())
                .location(toLocationDto(result.getLocation()))
                .build();
    }

    private AuthResponse.LocationDto toLocationDto(GeoLocation location) {
        if (location == null) return null;
        return AuthResponse.LocationDto.builder()
                .ip(location.getIp())
                .city(location.getCity())
                .country(location.getCountry())
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .build();
    }

    /**
     * Détermine l'IP réelle du client en tenant compte des reverse proxies.
     * Priorité : X-Forwarded-For (première IP de la chaîne) → X-Real-IP → adresse distante directe.
     */
    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
