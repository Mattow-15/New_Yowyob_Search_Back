package com.yowyob.listing.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Modèle de domaine d'un avis (Review).
 *
 * RÈGLE D'OR : aucune annotation Spring, JPA ou Jackson.
 * Le lien vers l'annonce est exprimé par un simple identifiant (listingId),
 * jamais par une référence à l'entité JPA (piège n°1 évité).
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Review {

    private UUID id;
    private Integer rating;
    private String comment;
    private String userId;
    private UUID listingId;
    private LocalDateTime createdAt;
}
