package com.yowyob.listing.infrastructure.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Interface Spring Data JPA — uniquement pour la persistance.
 * Ne doit jamais être utilisée directement hors de PostgresListingAdapter.
 */
public interface ListingJpaRepository extends JpaRepository<ListingJpaEntity, UUID> {

    Optional<ListingJpaEntity> findByOsmId(String osmId);

    List<ListingJpaEntity> findBySellerId(UUID sellerId);

    List<ListingJpaEntity> findByUpdatedAtAfter(LocalDateTime updatedAt);
}
