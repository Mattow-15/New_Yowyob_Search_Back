package com.yowyob.listing.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Modèle de domaine d'une annonce (Listing).
 *
 * RÈGLE D'OR : cette classe ne contient AUCUNE annotation Spring, JPA ou Jackson.
 * Elle représente uniquement la réalité métier — jamais un détail technique.
 *
 * Note : @Builder(toBuilder = true) permet la logique d'upsert dans le domaine
 * sans dépendre du toBuilder() de l'entité JPA (piège n°2).
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Listing {

    private UUID id;

    /** Identifiant externe (ex: osmId du crawler) — sert à l'upsert idempotent */
    private String externalId;
    private String osmId;

    private String title;
    private String description;
    private Double price;
    private String category;
    private UUID sellerId;

    private String address;
    private Double latitude;
    private Double longitude;

    private String imageUrl;
    private String phone;
    private String website;
    private String openingHours;

    private Double averageRating;
    private Integer reviewCount;
    private Double rating;
    private Integer reviewsCount;

    private ListingStatus status;

    /** Origine de la donnée : KERNEL_ORG, OSM, GOOGLE_PLACES, BUSINESS_BOOK, etc. */
    private String source;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
