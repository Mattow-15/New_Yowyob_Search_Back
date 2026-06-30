package com.yowyob.listing.application.ports.out;

import com.yowyob.listing.domain.model.Review;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * PORT SORTANT — Contrat de persistance des avis vu par le domaine.
 *
 * RÈGLE D'OR : toutes les méthodes manipulent uniquement des objets
 * du domaine (Review). Aucune trace de JPA, Hibernate ou Spring Data.
 * L'adaptateur PostgreSQL implémentera ce contrat et s'occupera du mapping.
 */
public interface ReviewRepositoryPort {

    Review save(Review review);

    List<Review> findByListingId(UUID listingId);

    Optional<Review> findById(UUID reviewId);

    void delete(Review review);

    boolean existsByListingIdAndUserId(UUID listingId, String userId);

    Optional<Double> calculateAverageRating(UUID listingId);

    long countByListingId(UUID listingId);
}
