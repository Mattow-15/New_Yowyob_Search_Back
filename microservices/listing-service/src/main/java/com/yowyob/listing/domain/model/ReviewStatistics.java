package com.yowyob.listing.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Modèle de domaine : statistiques des avis d'une annonce.
 * Valeur de lecture pure — aucune dépendance technique.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewStatistics {

    private Double averageRating;
    private Integer reviewCount;
    /** Distribution des notes 1→5, ex: {1:2, 2:0, 3:5, 4:12, 5:8} */
    private Map<Integer, Long> ratingDistribution;
}
