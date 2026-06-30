package com.yowyob.user.infrastructure.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
interface SearchHistoryJpaRepository extends JpaRepository<SearchHistoryJpaEntity, UUID> {
    List<SearchHistoryJpaEntity> findByUserIdOrderBySearchedAtDesc(UUID userId);
    void deleteByUserId(UUID userId);
}
