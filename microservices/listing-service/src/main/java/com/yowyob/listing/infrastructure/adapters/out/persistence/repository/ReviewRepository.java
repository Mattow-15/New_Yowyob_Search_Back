package com.yowyob.listing.infrastructure.adapters.out.persistence.repository;

import com.yowyob.listing.infrastructure.adapters.out.persistence.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    // Tous les avis d'un commerce, du plus récent au plus ancien
    List<Review> findByListingIdOrderByCreatedAtDesc(UUID listingId);

    // Calcul de la moyenne directement en base
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.listing.id = :listingId")
    Optional<Double> calculateAverageRating(@Param("listingId") UUID listingId);

    // Nombre d'avis pour un commerce
    long countByListingId(UUID listingId);

    // Vérifie si un utilisateur a déjà noté ce commerce
    boolean existsByListingIdAndUserId(UUID listingId, String userId);
}
