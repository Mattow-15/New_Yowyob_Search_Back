package com.yowyob.listing.infrastructure.adapters.out.persistence;

import com.yowyob.listing.domain.model.ListingStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ENTITÉ JPA — uniquement pour la persistance PostgreSQL.
 *
 * Cette classe est distincte du modèle de domaine (Listing).
 * RÈGLE : ne jamais utiliser cette classe en dehors du package
 * infrastructure.adapters.out.persistence.
 *
 * Le mapping entre cette entité et le domaine se fait
 * EXCLUSIVEMENT dans PostgresListingAdapter.
 */
@Entity
@Table(name = "listings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListingJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "external_id", unique = true)
    private String externalId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Double price;

    @Column(nullable = false)
    private String category;

    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;

    private String address;
    private Double latitude;
    private Double longitude;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "website", length = 2048)
    private String website;

    @Column(name = "opening_hours", length = 512)
    private String openingHours;

    @Column(nullable = false)
    @Builder.Default
    private Double averageRating = 0.0;

    @Column(nullable = false)
    @Builder.Default
    private Integer reviewCount = 0;

    @Column(unique = true)
    private String osmId;

    @Column(name = "rating")
    private Double rating;

    @Column(name = "reviews_count")
    private Integer reviewsCount;

    @Enumerated(EnumType.STRING)
    private ListingStatus status;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
