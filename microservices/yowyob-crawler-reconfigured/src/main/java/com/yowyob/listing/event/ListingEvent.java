package com.yowyob.listing.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.UUID;

/**
 * Événement Kafka représentant un commerce découvert ou mis à jour.
 * Publié par le crawler-service → consommé par le listing-service et search-service.
 *
 * Champs hérités : compatibilité avec le listing-service existant.
 * Champs Google Places : données spécifiques à l'API Google.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ListingEvent {

    // ─── Champs hérités (compatibilité listing-service) ───────────────────

    private UUID id;

    /** Nom du commerce (mappé depuis place.name). */
    private String title;

    /** Description générée automatiquement (types + statut). */
    private String description;

    /** Prix moyen estimé depuis le price_level Google (peut être null). */
    private Double price;

    /** Catégorie principale (premier type Google Places). */
    private String category;

    /** Adresse complète formatée. */
    private String address;

    private Double latitude;
    private Double longitude;

    /** Statut du listing : ACTIVE, INACTIVE. */
    private String status;

    /** ID du vendeur/propriétaire — null pour les commerces crawlés. */
    private UUID sellerId;

    /**
     * Type d'événement Kafka.
     * BRAND_NEW : premier crawl de ce commerce.
     * UPDATE     : données mises à jour.
     * DELETED    : commerce disparu.
     */
    private String eventType;

    // ─── Champs spécifiques Google Places ─────────────────────────────────

    /** Identifiant unique Google Places — utilisé pour la déduplication. */
    private String placeId;

    /** Note Google (0.0 à 5.0). Null si aucun avis. */
    private Double rating;

    /** Nombre total d'avis Google. */
    private Integer reviewCount;

    /** Numéro de téléphone international (ex: +237 222 220 000). */
    private String phone;

    /** URL du site web du commerce. */
    private String website;

    /** Indique si le commerce est ouvert au moment du crawl. */
    private Boolean openNow;

    /**
     * Statut opérationnel Google.
     * Valeurs : OPERATIONAL, CLOSED_TEMPORARILY, CLOSED_PERMANENTLY
     */
    private String businessStatus;

    /** Nom de la ville crawlée (ex: "Yaoundé"). */
    private String sourceCity;

    /** Timestamp ISO-8601 de découverte du commerce. */
    private String discoveredAt;

    /** Types Google Places complets (ex: ["restaurant", "food"]). */
    private List<String> allTypes;

    /**
     * Référence photo Google (utilisable pour construire une URL d'image).
     * Format URL : https://maps.googleapis.com/maps/api/place/photo
     *              ?maxwidth=400&photo_reference=REF&key=API_KEY
     */
    private String photoReference;
}
