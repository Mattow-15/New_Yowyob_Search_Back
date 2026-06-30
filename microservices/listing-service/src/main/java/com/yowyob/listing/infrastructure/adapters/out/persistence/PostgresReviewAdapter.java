package com.yowyob.listing.infrastructure.adapters.out.persistence;

import com.yowyob.listing.application.ports.out.ReviewRepositoryPort;
import com.yowyob.listing.domain.model.Review;
import com.yowyob.listing.infrastructure.adapters.out.persistence.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * ADAPTATEUR SORTANT — PostgreSQL pour les avis.
 *
 * Implémente {@link ReviewRepositoryPort}. Tout le mapping
 * Domaine (Review) ↔ JPA (entity.Review) se fait ICI et UNIQUEMENT ICI.
 * Le lien vers l'annonce est résolu via une référence JPA paresseuse.
 */
@Component
@RequiredArgsConstructor
public class PostgresReviewAdapter implements ReviewRepositoryPort {

    private final ReviewRepository jpaRepository;
    private final ListingJpaRepository listingJpaRepository;

    @Override
    @Transactional
    public Review save(Review review) {
        com.yowyob.listing.infrastructure.adapters.out.persistence.entity.Review entity =
                toJpaEntity(review);
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Review> findByListingId(UUID listingId) {
        return jpaRepository.findByListingIdOrderByCreatedAtDesc(listingId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Review> findById(UUID reviewId) {
        return jpaRepository.findById(reviewId).map(this::toDomain);
    }

    @Override
    @Transactional
    public void delete(Review review) {
        jpaRepository.findById(review.getId()).ifPresent(jpaRepository::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByListingIdAndUserId(UUID listingId, String userId) {
        return jpaRepository.existsByListingIdAndUserId(listingId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Double> calculateAverageRating(UUID listingId) {
        return jpaRepository.calculateAverageRating(listingId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByListingId(UUID listingId) {
        return jpaRepository.countByListingId(listingId);
    }

    // ─── MAPPING À LA MAIN ────────────────────────────────────────────────────

    private com.yowyob.listing.infrastructure.adapters.out.persistence.entity.Review toJpaEntity(Review review) {
        ListingJpaEntity listingRef = listingJpaRepository.getReferenceById(review.getListingId());
        return com.yowyob.listing.infrastructure.adapters.out.persistence.entity.Review.builder()
                .id(review.getId())
                .rating(review.getRating())
                .comment(review.getComment())
                .userId(review.getUserId())
                .createdAt(review.getCreatedAt())
                .listing(listingRef)
                .build();
        // createdAt est géré par @PrePersist côté entité si null
    }

    private Review toDomain(com.yowyob.listing.infrastructure.adapters.out.persistence.entity.Review entity) {
        return Review.builder()
                .id(entity.getId())
                .rating(entity.getRating())
                .comment(entity.getComment())
                .userId(entity.getUserId())
                .listingId(entity.getListing() != null ? entity.getListing().getId() : null)
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
