package com.yowyob.user.repository;

import com.yowyob.user.entity.SearchHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Repository JPA pour l'entité {@link SearchHistory} (module user).
 * Fournit les requêtes d'historique de recherche par utilisateur.
 *
 * @author YowYob Team
 * @since 1.0.0
 */
public interface SearchHistoryRepository extends JpaRepository<SearchHistory, UUID> {
    List<SearchHistory> findByUserIdOrderBySearchedAtDesc(UUID userId);

    void deleteByUserId(UUID userId);
}
