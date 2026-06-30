package com.yowyob.auth.infrastructure.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {
    Optional<UserJpaEntity> findByEmail(String email);
    Boolean existsByEmail(String email);
}
