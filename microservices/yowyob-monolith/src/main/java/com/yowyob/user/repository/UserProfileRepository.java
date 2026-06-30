package com.yowyob.user.repository;

import com.yowyob.user.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository JPA pour l'entité {@link UserProfile}.
 * Fournit les requêtes de profil utilisateur par userId et par date de mise à
 * jour.
 *
 * @author YowYob Team
 * @since 1.0.0
 */
@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {
    Optional<UserProfile> findByUserId(UUID userId);

    java.util.List<UserProfile> findByUpdatedAtAfter(java.time.LocalDateTime updatedAt);
}
