package com.yowyob.listing.infrastructure.adapters.out.persistence;

import com.yowyob.listing.application.ports.out.ListingRepositoryPort;
import com.yowyob.listing.domain.model.Listing;
import com.yowyob.listing.domain.model.ListingStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * ADAPTATEUR SORTANT — PostgreSQL.
 *
 * Implémente ListingRepositoryPort : le seul point de contact entre le
 * domaine et la base de données. Tout le mapping Domaine ↔ JPA se fait
 * ICI et UNIQUEMENT ICI (piège n°1 évité).
 *
 * @Transactional sur save() : garantit que si le save() échoue, la
 * transaction est rollbackée et l'événement RabbitMQ ne sera pas publié
 * (piège n°3 évité — l'ApplicationService publie l'événement après le save).
 */
@Component
@RequiredArgsConstructor
public class PostgresListingAdapter implements ListingRepositoryPort {

    private final ListingJpaRepository jpaRepository;

    @Override
    @Transactional
    public Listing save(Listing listing) {
        ListingJpaEntity entity = toJpaEntity(listing);
        ListingJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Listing> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Listing> findByOsmId(String osmId) {
        return jpaRepository.findByOsmId(osmId).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Listing> findAll() {
        return jpaRepository.findAll().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Listing> findBySellerId(UUID sellerId) {
        return jpaRepository.findBySellerId(sellerId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Listing> findByUpdatedAtAfter(LocalDateTime dateTime) {
        return jpaRepository.findByUpdatedAtAfter(dateTime).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void delete(Listing listing) {
        jpaRepository.findById(listing.getId())
                .ifPresent(jpaRepository::delete);
    }

    // ─── MAPPING À LA MAIN (piège n°1) ───────────────────────────────────────
    // Ce bloc est le seul endroit dans tout le projet où l'on sait que
    // Listing (domaine) et ListingJpaEntity (infra) existent en même temps.

    private ListingJpaEntity toJpaEntity(Listing listing) {
        return ListingJpaEntity.builder()
                .id(listing.getId())
                .externalId(listing.getExternalId())
                .osmId(listing.getOsmId())
                .title(listing.getTitle())
                .description(listing.getDescription())
                .price(listing.getPrice() != null ? listing.getPrice() : 0.0)
                .category(listing.getCategory() != null ? listing.getCategory() : "GENERAL")
                .sellerId(listing.getSellerId())
                .address(listing.getAddress())
                .latitude(listing.getLatitude())
                .longitude(listing.getLongitude())
                .imageUrl(listing.getImageUrl())
                .phone(listing.getPhone())
                .website(listing.getWebsite())
                .openingHours(listing.getOpeningHours())
                .averageRating(listing.getAverageRating() != null ? listing.getAverageRating() : 0.0)
                .reviewCount(listing.getReviewCount() != null ? listing.getReviewCount() : 0)
                .rating(listing.getRating())
                .reviewsCount(listing.getReviewsCount())
                .status(listing.getStatus())
                .build();
        // Note : createdAt et updatedAt sont gérés par @CreationTimestamp/@UpdateTimestamp de Hibernate
    }

    private Listing toDomain(ListingJpaEntity entity) {
        return Listing.builder()
                .id(entity.getId())
                .externalId(entity.getExternalId())
                .osmId(entity.getOsmId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .price(entity.getPrice())
                .category(entity.getCategory())
                .sellerId(entity.getSellerId())
                .address(entity.getAddress())
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .imageUrl(entity.getImageUrl())
                .phone(entity.getPhone())
                .website(entity.getWebsite())
                .openingHours(entity.getOpeningHours())
                .averageRating(entity.getAverageRating())
                .reviewCount(entity.getReviewCount())
                .rating(entity.getRating())
                .reviewsCount(entity.getReviewsCount())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
