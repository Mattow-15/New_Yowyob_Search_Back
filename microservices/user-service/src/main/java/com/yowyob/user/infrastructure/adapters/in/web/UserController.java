package com.yowyob.user.infrastructure.adapters.in.web;

import com.yowyob.user.application.ports.in.ManageSearchHistoryUseCase;
import com.yowyob.user.application.ports.in.ManageUserUseCase;
import com.yowyob.user.domain.model.SearchHistory;
import com.yowyob.user.domain.model.UserProfile;
import com.yowyob.user.infrastructure.adapters.in.web.dto.SearchHistoryDto;
import com.yowyob.user.infrastructure.adapters.in.web.dto.UserProfileDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Service", description = "Operations for extended user profiles")
public class UserController {

    private final ManageUserUseCase manageUserUseCase;
    private final ManageSearchHistoryUseCase manageSearchHistoryUseCase;

    @Operation(summary = "Get My Profile", description = "Get profile of the currently authenticated user")
    @GetMapping("/me")
    public ResponseEntity<UserProfile> getMyProfile(@RequestHeader("X-User-Id") String userId) {
        if (userId == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(manageUserUseCase.getOrCreateProfile(UUID.fromString(userId)));
    }

    @Operation(summary = "Update My Profile", description = "Update profile of the currently authenticated user")
    @PutMapping("/me")
    public ResponseEntity<UserProfile> updateMyProfile(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody UserProfileDto dto) {
        if (userId == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        UserProfile profileDetails = UserProfile.builder()
                .firstName(dto.getFirstName())
                .email(dto.getEmail())
                .lastName(dto.getLastName())
                .bio(dto.getBio())
                .phoneNumber(dto.getPhoneNumber())
                .address(dto.getAddress())
                .city(dto.getCity())
                .country(dto.getCountry())
                .avatarUrl(dto.getAvatarUrl())
                .socialLinksJson(dto.getSocialLinksJson())
                .build();

        return ResponseEntity.ok(manageUserUseCase.updateProfile(UUID.fromString(userId), profileDetails));
    }

    @Operation(summary = "Get Public Profile", description = "Get public profile by User ID")
    @GetMapping("/{userId}")
    public ResponseEntity<UserProfile> getUserProfile(@PathVariable UUID userId) {
        return ResponseEntity.ok(manageUserUseCase.getOrCreateProfile(userId));
    }

    @Operation(summary = "Get All Users", description = "Get all user profiles (For Crawler)")
    @GetMapping
    public ResponseEntity<List<UserProfile>> getAllUsers() {
        return ResponseEntity.ok(manageUserUseCase.findAllProfiles());
    }

    @Operation(summary = "Search Documents", description = "Internal endpoint for Crawler Service to fetch documents")
    @GetMapping("/search/documents")
    public ResponseEntity<List<UserProfile>> searchDocuments(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime updatedAfter) {
        return ResponseEntity.ok(manageUserUseCase.searchProfiles(updatedAfter));
    }

    // --- Search History ---

    @Operation(summary = "Add Search History", description = "Add a query to user's search history")
    @PostMapping("/history")
    public ResponseEntity<Void> addSearchHistory(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody SearchHistoryDto dto) {
        if (userId == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        manageSearchHistoryUseCase.addSearchHistory(UUID.fromString(userId), dto.getQuery());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get Search History", description = "Get user's search history")
    @GetMapping("/history")
    public ResponseEntity<List<SearchHistory>> getSearchHistory(
            @RequestHeader("X-User-Id") String userId) {
        if (userId == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(manageSearchHistoryUseCase.getSearchHistory(UUID.fromString(userId)));
    }

    @Operation(summary = "Clear Search History", description = "Clear user's search history")
    @DeleteMapping("/history")
    public ResponseEntity<Void> clearSearchHistory(@RequestHeader("X-User-Id") String userId) {
        if (userId == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        manageSearchHistoryUseCase.clearSearchHistory(UUID.fromString(userId));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/health")
    public String health() {
        return "User Service is running!";
    }
}
