package com.yowyob.listing.infrastructure.adapters.out.persistence.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.yowyob.listing.infrastructure.adapters.out.persistence.ListingJpaEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reviews")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    @Min(value = 1, message = "La note minimum est 1")
    @Max(value = 5, message = "La note maximum est 5")
    private Integer rating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(nullable = false)
    private String userId;          // identifiant de l'auteur

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    @JsonIgnore
    private ListingJpaEntity listing;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
