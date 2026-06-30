package com.yowyob.user.application.ports.out;

import com.yowyob.user.domain.model.UserProfile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepositoryPort {
    Optional<UserProfile> findByUserId(UUID userId);
    UserProfile save(UserProfile userProfile);
    List<UserProfile> findAll();
    List<UserProfile> findByUpdatedAtAfter(LocalDateTime updatedAfter);
}
