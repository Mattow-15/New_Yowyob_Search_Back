package com.yowyob.listing.repository;

import com.yowyob.listing.entity.Listing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository JPA pour l'entité {@link Listing}.
 * Fournit les requêtes de recherche par vendeur et par date de mise à jour.
 *
 * @author YowYob Team
 * @since 1.0.0
 */
@Repository
public interface ListingRepository extends JpaRepository<Listing, UUID> {
    List<Listing> findBySellerId(UUID sellerId);

    List<Listing> findByUpdatedAtAfter(java.time.LocalDateTime updatedAt);
}
