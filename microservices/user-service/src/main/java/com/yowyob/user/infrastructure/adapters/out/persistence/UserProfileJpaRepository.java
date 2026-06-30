package com.yowyob.user.infrastructure.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
interface UserProfileJpaRepository extends JpaRepository<UserProfileJpaEntity, UUID> {
    Optional<UserProfileJpaEntity> findByUserId(UUID userId);
    List<UserProfileJpaEntity> findByUpdatedAtAfter(LocalDateTime updatedAt);
}
