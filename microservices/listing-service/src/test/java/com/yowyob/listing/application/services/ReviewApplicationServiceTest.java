package com.yowyob.listing.application.services;

import com.yowyob.listing.application.ports.out.ListingRepositoryPort;
import com.yowyob.listing.application.ports.out.ReviewRepositoryPort;
import com.yowyob.listing.domain.exception.ListingNotFoundException;
import com.yowyob.listing.domain.model.Listing;
import com.yowyob.listing.domain.model.Review;
import com.yowyob.listing.domain.model.ReviewStatistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires du cas d'usage Review, en architecture hexagonale :
 * le service ne dépend que des PORTS (mockés) et de modèles de DOMAINE.
 */
@ExtendWith(MockitoExtension.class)
class ReviewApplicationServiceTest {

    @Mock
    private ReviewRepositoryPort reviewRepository;

    @Mock
    private ListingRepositoryPort listingRepository;

    private ReviewApplicationService reviewService;

    private Listing mockListing;
    private UUID listingId;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewApplicationService(reviewRepository, listingRepository);
        listingId = UUID.randomUUID();
        mockListing = new Listing();
        mockListing.setId(listingId);
        mockListing.setTitle("Restaurant Test");
        mockListing.setAverageRating(0.0);
        mockListing.setReviewCount(0);
    }

    @Test
    @DisplayName("Doit créer un avis et mettre à jour la moyenne")
    void createReview_shouldSaveAndUpdateRating() {
        // ARRANGE
        Review request = Review.builder()
                .rating(5).comment("Excellent !").userId("user_001").listingId(listingId)
                .build();

        when(listingRepository.findById(listingId)).thenReturn(Optional.of(mockListing));
        when(reviewRepository.existsByListingIdAndUserId(listingId, "user_001")).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reviewRepository.calculateAverageRating(listingId)).thenReturn(Optional.of(5.0));
        when(reviewRepository.countByListingId(listingId)).thenReturn(1L);

        // ACT
        Review created = reviewService.createReview(request);

        // ASSERT
        assertThat(created.getRating()).isEqualTo(5);
        assertThat(created.getComment()).isEqualTo("Excellent !");
        verify(reviewRepository, times(1)).save(any(Review.class));
        verify(listingRepository, times(1)).save(mockListing);
        assertThat(mockListing.getAverageRating()).isEqualTo(5.0);
        assertThat(mockListing.getReviewCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Doit rejeter un doublon d'avis du même utilisateur")
    void createReview_shouldRejectDuplicateFromSameUser() {
        // ARRANGE
        Review request = Review.builder()
                .rating(3).comment("Deuxième tentative").userId("user_001").listingId(listingId)
                .build();

        when(listingRepository.findById(listingId)).thenReturn(Optional.of(mockListing));
        when(reviewRepository.existsByListingIdAndUserId(listingId, "user_001")).thenReturn(true);

        // ACT & ASSERT
        assertThatThrownBy(() -> reviewService.createReview(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("déjà soumis un avis");

        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("Doit calculer correctement la moyenne et la distribution")
    void getSummary_shouldCalculateCorrectAverage() {
        // ARRANGE
        mockListing.setAverageRating(4.0);
        mockListing.setReviewCount(2);

        Review r1 = Review.builder().rating(5).userId("u1").listingId(listingId).build();
        Review r2 = Review.builder().rating(3).userId("u2").listingId(listingId).build();

        when(listingRepository.findById(listingId)).thenReturn(Optional.of(mockListing));
        when(reviewRepository.findByListingId(listingId)).thenReturn(List.of(r1, r2));

        // ACT
        ReviewStatistics summary = reviewService.getSummary(listingId);

        // ASSERT
        assertThat(summary.getAverageRating()).isEqualTo(4.0);
        assertThat(summary.getReviewCount()).isEqualTo(2);
        assertThat(summary.getRatingDistribution().get(5)).isEqualTo(1L);
        assertThat(summary.getRatingDistribution().get(3)).isEqualTo(1L);
        assertThat(summary.getRatingDistribution().get(1)).isEqualTo(0L);
    }

    @Test
    @DisplayName("Doit lever une exception métier si le commerce n'existe pas")
    void createReview_shouldThrowIfListingNotFound() {
        // ARRANGE
        when(listingRepository.findById(any())).thenReturn(Optional.empty());

        Review request = Review.builder()
                .rating(4).comment("Test").userId("user_001").listingId(UUID.randomUUID())
                .build();

        // ACT & ASSERT
        assertThatThrownBy(() -> reviewService.createReview(request))
                .isInstanceOf(ListingNotFoundException.class);
    }
}
