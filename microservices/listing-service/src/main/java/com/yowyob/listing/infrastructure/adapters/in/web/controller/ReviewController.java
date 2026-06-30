package com.yowyob.listing.infrastructure.adapters.in.web.controller;

import com.yowyob.listing.application.ports.in.ManageReviewsUseCase;
import com.yowyob.listing.domain.model.Review;
import com.yowyob.listing.domain.model.ReviewStatistics;
import com.yowyob.listing.infrastructure.adapters.in.web.dto.ReviewRequest;
import com.yowyob.listing.infrastructure.adapters.in.web.dto.ReviewResponse;
import com.yowyob.listing.infrastructure.adapters.in.web.dto.ReviewSummary;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * ADAPTATEUR ENTRANT — REST.
 *
 * Dépend uniquement du port {@link ManageReviewsUseCase}. Tout le mapping
 * DTO web ↔ modèle de domaine se fait ICI ; le domaine ignore l'existence
 * de ReviewRequest / ReviewResponse / ReviewSummary.
 */
@RestController
@RequestMapping("/api/listings/{listingId}/reviews")
@RequiredArgsConstructor
@Slf4j
public class ReviewController {

    private final ManageReviewsUseCase manageReviewsUseCase;

    // POST /api/listings/{listingId}/reviews
    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(
            @PathVariable UUID listingId,
            @RequestBody @Valid ReviewRequest request) {

        Review created = manageReviewsUseCase.createReview(toDomain(listingId, request));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    // GET /api/listings/{listingId}/reviews
    @GetMapping
    public ResponseEntity<List<ReviewResponse>> getReviews(@PathVariable UUID listingId) {
        List<ReviewResponse> responses = manageReviewsUseCase.getReviews(listingId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    // GET /api/listings/{listingId}/reviews/summary
    @GetMapping("/summary")
    public ResponseEntity<ReviewSummary> getSummary(@PathVariable UUID listingId) {
        return ResponseEntity.ok(toSummary(manageReviewsUseCase.getSummary(listingId)));
    }

    // DELETE /api/listings/{listingId}/reviews/{reviewId}
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable UUID listingId,
            @PathVariable UUID reviewId,
            @RequestParam String userId) {

        manageReviewsUseCase.deleteReview(listingId, reviewId, userId);
        return ResponseEntity.noContent().build();
    }

    // ─── MAPPING DTO ↔ DOMAINE ────────────────────────────────────────────────

    private Review toDomain(UUID listingId, ReviewRequest request) {
        return Review.builder()
                .rating(request.getRating())
                .comment(request.getComment())
                .userId(request.getUserId())
                .listingId(listingId)
                .build();
    }

    private ReviewResponse toResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .rating(review.getRating())
                .comment(review.getComment())
                .userId(review.getUserId())
                .createdAt(review.getCreatedAt())
                .build();
    }

    private ReviewSummary toSummary(ReviewStatistics stats) {
        return ReviewSummary.builder()
                .averageRating(stats.getAverageRating())
                .reviewCount(stats.getReviewCount())
                .ratingDistribution(stats.getRatingDistribution())
                .build();
    }
}
