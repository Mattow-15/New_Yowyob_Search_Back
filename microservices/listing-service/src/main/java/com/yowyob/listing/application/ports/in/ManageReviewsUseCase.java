package com.yowyob.listing.application.ports.in;

import com.yowyob.listing.domain.model.Review;
import com.yowyob.listing.domain.model.ReviewStatistics;

import java.util.List;
import java.util.UUID;

/**
 * PORT ENTRANT — Gestion des avis exposée via l'API REST.
 * Le ReviewController dépendra de cette interface, jamais du service directement.
 *
 * RÈGLE D'OR : la signature ne manipule que des modèles de domaine
 * (Review, ReviewStatistics), jamais de DTO web ni d'entité JPA.
 */
public interface ManageReviewsUseCase {

    /** Crée un avis (review.listingId doit être renseigné). */
    Review createReview(Review review);

    List<Review> getReviews(UUID listingId);

    ReviewStatistics getSummary(UUID listingId);

    void deleteReview(UUID listingId, UUID reviewId, String userId);
}
