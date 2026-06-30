package com.yowyob.user.application.services;

import com.yowyob.user.application.ports.in.ManageSearchHistoryUseCase;
import com.yowyob.user.application.ports.in.ManageUserUseCase;
import com.yowyob.user.application.ports.out.SearchHistoryRepositoryPort;
import com.yowyob.user.application.ports.out.UserRepositoryPort;
import com.yowyob.user.domain.model.SearchHistory;
import com.yowyob.user.domain.model.UserProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class UserApplicationService implements ManageUserUseCase, ManageSearchHistoryUseCase {

    private final UserRepositoryPort userRepositoryPort;
    private final SearchHistoryRepositoryPort searchHistoryRepositoryPort;

    @Override
    public UserProfile getOrCreateProfile(UUID userId) {
        return userRepositoryPort.findByUserId(userId)
                .orElseGet(() -> {
                    UserProfile newProfile = UserProfile.builder()
                            .userId(userId)
                            .build();
                    return userRepositoryPort.save(newProfile);
                });
    }

    @Override
    public UserProfile updateProfile(UUID userId, UserProfile profileDetails) {
        UserProfile profile = getOrCreateProfile(userId);

        // Update fields by copying values if they are not null in profileDetails
        UserProfile.UserProfileBuilder updatedBuilder = profile.toBuilder();

        if (profileDetails.getFirstName() != null) updatedBuilder.firstName(profileDetails.getFirstName());
        if (profileDetails.getEmail() != null) updatedBuilder.email(profileDetails.getEmail());
        if (profileDetails.getLastName() != null) updatedBuilder.lastName(profileDetails.getLastName());
        if (profileDetails.getBio() != null) updatedBuilder.bio(profileDetails.getBio());
        if (profileDetails.getPhoneNumber() != null) updatedBuilder.phoneNumber(profileDetails.getPhoneNumber());
        if (profileDetails.getAddress() != null) updatedBuilder.address(profileDetails.getAddress());
        if (profileDetails.getCity() != null) updatedBuilder.city(profileDetails.getCity());
        if (profileDetails.getCountry() != null) updatedBuilder.country(profileDetails.getCountry());
        if (profileDetails.getAvatarUrl() != null) updatedBuilder.avatarUrl(profileDetails.getAvatarUrl());
        if (profileDetails.getSocialLinksJson() != null) updatedBuilder.socialLinksJson(profileDetails.getSocialLinksJson());

        return userRepositoryPort.save(updatedBuilder.build());
    }

    @Override
    public List<UserProfile> findAllProfiles() {
        return userRepositoryPort.findAll();
    }

    @Override
    public List<UserProfile> searchProfiles(LocalDateTime updatedAfter) {
        if (updatedAfter == null) {
            return userRepositoryPort.findAll();
        }
        return userRepositoryPort.findByUpdatedAtAfter(updatedAfter);
    }

    @Override
    public void addSearchHistory(UUID userId, String query) {
        if (query == null || query.isBlank()) {
            return;
        }
        SearchHistory history = SearchHistory.builder()
                .userId(userId)
                .query(query.trim())
                .searchedAt(LocalDateTime.now())
                .build();
        searchHistoryRepositoryPort.save(history);
    }

    @Override
    public List<SearchHistory> getSearchHistory(UUID userId) {
        return searchHistoryRepositoryPort.findByUserIdOrderBySearchedAtDesc(userId);
    }

    @Override
    public void clearSearchHistory(UUID userId) {
        searchHistoryRepositoryPort.deleteByUserId(userId);
    }
}
