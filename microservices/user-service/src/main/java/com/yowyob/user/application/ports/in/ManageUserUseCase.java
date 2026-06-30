package com.yowyob.user.application.ports.in;

import com.yowyob.user.domain.model.UserProfile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ManageUserUseCase {
    UserProfile getOrCreateProfile(UUID userId);
    UserProfile updateProfile(UUID userId, UserProfile profileDetails);
    List<UserProfile> findAllProfiles();
    List<UserProfile> searchProfiles(LocalDateTime updatedAfter);
}
