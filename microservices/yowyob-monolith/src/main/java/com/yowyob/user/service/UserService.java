package com.yowyob.user.service;

import com.yowyob.user.dto.UserProfileDto;
import com.yowyob.user.entity.UserProfile;
import com.yowyob.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service de gestion des profils utilisateurs et de l'historique de recherche.
 * Fournit les opérations CRUD sur les profils et la gestion de l'historique.
 *
 * @author YowYob Team
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserProfileRepository userProfileRepository;

    public UserProfile getOrCreateProfile(UUID userId) {
        return userProfileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserProfile newProfile = UserProfile.builder()
                            .userId(userId)
                            .build();
                    return userProfileRepository.save(newProfile);
                });
    }

    public UserProfile updateProfile(UUID userId, UserProfileDto dto) {
        UserProfile profile = getOrCreateProfile(userId);

        if (dto.getFirstName() != null)
            profile.setFirstName(dto.getFirstName());
        if (dto.getEmail() != null)
            profile.setEmail(dto.getEmail());
        if (dto.getLastName() != null)
            profile.setLastName(dto.getLastName());
        if (dto.getBio() != null)
            profile.setBio(dto.getBio());
        if (dto.getPhoneNumber() != null)
            profile.setPhoneNumber(dto.getPhoneNumber());
        if (dto.getAddress() != null)
            profile.setAddress(dto.getAddress());
        if (dto.getCity() != null)
            profile.setCity(dto.getCity());
        if (dto.getCountry() != null)
            profile.setCountry(dto.getCountry());
        if (dto.getAvatarUrl() != null)
            profile.setAvatarUrl(dto.getAvatarUrl());
        if (dto.getSocialLinksJson() != null)
            profile.setSocialLinksJson(dto.getSocialLinksJson());

        return userProfileRepository.save(profile);
    }

    public java.util.List<UserProfile> findAllProfiles() {
        return userProfileRepository.findAll();
    }

    public java.util.List<UserProfile> searchProfiles(java.time.LocalDateTime updatedAfter) {
        if (updatedAfter == null) {
            return userProfileRepository.findAll();
        }
        return userProfileRepository.findByUpdatedAtAfter(updatedAfter);
    }

    // History
    private final com.yowyob.user.repository.SearchHistoryRepository searchHistoryRepository;

    public void addSearchHistory(UUID userId, String query) {
        if (query == null || query.isBlank())
            return;
        // Optional: Avoid consecutive duplicates
        // But for now, just save as requested
        com.yowyob.user.entity.SearchHistory history = com.yowyob.user.entity.SearchHistory.builder()
                .userId(userId)
                .query(query.trim())
                .build();
        searchHistoryRepository.save(history);
    }

    public java.util.List<com.yowyob.user.entity.SearchHistory> getSearchHistory(UUID userId) {
        return searchHistoryRepository.findByUserIdOrderBySearchedAtDesc(userId);
    }

    @org.springframework.transaction.annotation.Transactional
    public void clearSearchHistory(UUID userId) {
        searchHistoryRepository.deleteByUserId(userId);
    }
}
