package com.yowyob.listing.application.services;

import com.yowyob.listing.application.ports.in.ManageReviewsUseCase;
import com.yowyob.listing.application.ports.out.ListingRepositoryPort;
import com.yowyob.listing.application.ports.out.ReviewRepositoryPort;
import com.yowyob.listing.domain.exception.ListingNotFoundException;
import com.yowyob.listing.domain.exception.ReviewNotFoundException;
import com.yowyob.listing.domain.model.Listing;
import com.yowyob.listing.domain.model.Review;
import com.yowyob.listing.domain.model.ReviewStatistics;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * SERVICE D'APPLICATION — Cas d'usage liés aux avis.
 *
 * RÈGLE D'OR : PAS d'annotation Spring ici (@Service, @Transactional, etc.).
 * Le câblage se fait dans {@code DomainConfig}. Le service ne connaît que
 * des ports (interfaces) et des modèles de domaine.
 */
public class ReviewApplicationService implements ManageReviewsUseCase {

    private final ReviewRepositoryPort reviewRepository;
    private final ListingRepositoryPort listingRepository;

    public ReviewApplicationService(ReviewRepositoryPort reviewRepository,
                                    ListingRepositoryPort listingRepository) {
        this.reviewRepository = reviewRepository;
        this.listingRepository = listingRepository;
    }

    @Override
    public Review createReview(Review review) {
        UUID listingId = review.getListingId();

        // 1. Vérifie que le commerce existe
        listingRepository.findById(listingId)
                .orElseThrow(() -> new ListingNotFoundException(listingId));

        // 2. Un utilisateur ne peut noter qu'une seule fois un commerce
        if (reviewRepository.existsByListingIdAndUserId(listingId, review.getUserId())) {
            throw new IllegalStateException(
                    "Vous avez déjà soumis un avis pour ce commerce");
        }

        // 3. Persiste l'avis
        Review saved = reviewRepository.save(review);

        // 4. Recalcule la moyenne du commerce
        updateListingRating(listingId);

        return saved;
    }

    @Override
    public List<Review> getReviews(UUID listingId) {
        listingRepository.findById(listingId)
                .orElseThrow(() -> new ListingNotFoundException(listingId));

        return reviewRepository.findByListingId(listingId);
    }

    @Override
    public ReviewStatistics getSummary(UUID listingId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ListingNotFoundException(listingId));

        Map<Integer, Long> distribution = reviewRepository.findByListingId(listingId).stream()
                .collect(Collectors.groupingBy(Review::getRating, Collectors.counting()));

        // S'assure que toutes les notes 1→5 sont présentes (même à 0)
        for (int i = 1; i <= 5; i++) {
            distribution.putIfAbsent(i, 0L);
        }

        return ReviewStatistics.builder()
                .averageRating(listing.getAverageRating())
                .reviewCount(listing.getReviewCount())
                .ratingDistribution(distribution)
                .build();
    }

    @Override
    public void deleteReview(UUID listingId, UUID reviewId, String userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId));

        if (!review.getUserId().equals(userId)) {
            throw new SecurityException("Vous ne pouvez supprimer que vos propres avis");
        }

        reviewRepository.delete(review);

        // Recalcule la moyenne après suppression
        updateListingRating(review.getListingId());
    }

    // ── Logique métier privée ────────────────────────────────────────────────

    private void updateListingRating(UUID listingId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ListingNotFoundException(listingId));

        double avg = reviewRepository.calculateAverageRating(listingId).orElse(0.0);
        long count = reviewRepository.countByListingId(listingId);

        // Arrondi à 1 décimale — ex: 4.3 au lieu de 4.333333
        listing.setAverageRating(Math.round(avg * 10.0) / 10.0);
        listing.setReviewCount((int) count);
        listingRepository.save(listing);
    }
}
