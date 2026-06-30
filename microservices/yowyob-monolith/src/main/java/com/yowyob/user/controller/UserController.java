package com.yowyob.user.controller;

import com.yowyob.user.dto.UserProfileDto;
import com.yowyob.user.entity.UserProfile;
import com.yowyob.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Contrôleur REST pour la gestion des profils utilisateurs et de l'historique
 * de recherche.
 * Expose les endpoints CRUD pour les profils et l'historique.
 *
 * @author YowYob Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Service", description = "Operations for extended user profiles")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Get My Profile", description = "Get profile of the currently authenticated user")
    @GetMapping("/me")
    public ResponseEntity<UserProfile> getMyProfile(@RequestHeader("X-User-Id") String userId) {
        if (userId == null)
            return ResponseEntity.status(401).build();
        return ResponseEntity.ok(userService.getOrCreateProfile(UUID.fromString(userId)));
    }

    @Operation(summary = "Update My Profile", description = "Update profile of the currently authenticated user")
    @PutMapping("/me")
    public ResponseEntity<UserProfile> updateMyProfile(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody UserProfileDto dto) {
        if (userId == null)
            return ResponseEntity.status(401).build();
        return ResponseEntity.ok(userService.updateProfile(UUID.fromString(userId), dto));
    }

    @Operation(summary = "Get Public Profile", description = "Get public profile by User ID")
    @GetMapping("/{userId}")
    public ResponseEntity<UserProfile> getUserProfile(@PathVariable UUID userId) {
        return ResponseEntity.ok(userService.getOrCreateProfile(userId));
    }

    @Operation(summary = "Get All Users", description = "Get all user profiles (For Crawler)")
    @GetMapping
    public ResponseEntity<java.util.List<UserProfile>> getAllUsers() {
        return ResponseEntity.ok(userService.findAllProfiles());
    }

    @Operation(summary = "Search Documents", description = "Internal endpoint for Crawler Service to fetch documents")
    @GetMapping("/search/documents")
    public ResponseEntity<java.util.List<UserProfile>> searchDocuments(
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime updatedAfter) {
        return ResponseEntity.ok(userService.searchProfiles(updatedAfter));
    }

    // --- Search History ---

    @Operation(summary = "Add Search History", description = "Add a query to user's search history")
    @PostMapping("/history")
    public ResponseEntity<Void> addSearchHistory(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody com.yowyob.user.dto.SearchHistoryDto dto) {
        if (userId == null)
            return ResponseEntity.status(401).build();
        userService.addSearchHistory(UUID.fromString(userId), dto.getQuery());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get Search History", description = "Get user's search history")
    @GetMapping("/history")
    public ResponseEntity<java.util.List<com.yowyob.user.entity.SearchHistory>> getSearchHistory(
            @RequestHeader("X-User-Id") String userId) {
        if (userId == null)
            return ResponseEntity.status(401).build();
        return ResponseEntity.ok(userService.getSearchHistory(UUID.fromString(userId)));
    }

    @Operation(summary = "Clear Search History", description = "Clear user's search history")
    @DeleteMapping("/history")
    public ResponseEntity<Void> clearSearchHistory(@RequestHeader("X-User-Id") String userId) {
        if (userId == null)
            return ResponseEntity.status(401).build();
        userService.clearSearchHistory(UUID.fromString(userId));
        return ResponseEntity.noContent().build();
    }

    // --- Stubbed Endpoints for Profile UI until Modules are fully developed ---

    @Operation(summary = "Get User Favorites", description = "Stubbed endpoint for favorites")
    @GetMapping("/favorites")
    public ResponseEntity<java.util.List<Object>> getUserFavorites(@RequestHeader("X-User-Id") String userId) {
        if (userId == null)
            return ResponseEntity.status(401).build();
        return ResponseEntity.ok(new java.util.ArrayList<>());
    }

    @Operation(summary = "Get User Messages Count", description = "Stubbed endpoint for messages count")
    @GetMapping("/messages/count")
    public ResponseEntity<java.util.Map<String, Integer>> getUserMessagesCount(@RequestHeader("X-User-Id") String userId) {
        if (userId == null)
            return ResponseEntity.status(401).build();
        return ResponseEntity.ok(java.util.Map.of("count", 0));
    }

    @GetMapping("/health")
    public String health() {
        return "User Service is running!";
    }
}
