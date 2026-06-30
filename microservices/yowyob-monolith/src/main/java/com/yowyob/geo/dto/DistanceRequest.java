package com.yowyob.geo.dto;

import lombok.Data;

/**
 * DTO pour une requête de calcul de distance entre deux points géographiques.
 *
 * @author YowYob Team
 * @since 1.0.0
 */
@Data
public class DistanceRequest {
    private Double lat1;
    private Double lon1;
    private Double lat2;
    private Double lon2;
}
