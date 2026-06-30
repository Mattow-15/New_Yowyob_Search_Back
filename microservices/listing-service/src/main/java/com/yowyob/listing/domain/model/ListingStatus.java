package com.yowyob.listing.domain.model;

/**
 * Statut d'une annonce au niveau du domaine.
 * Aucune dépendance Spring ou JPA.
 */
public enum ListingStatus {
    ACTIVE,
    SOLD,
    INACTIVE,
    PENDING,
    DELETED
}
