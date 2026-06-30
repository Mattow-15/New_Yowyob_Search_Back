package com.yowyob.geo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * DTO de réponse contenant la distance calculée en kilomètres et en miles.
 *
 * @author YowYob Team
 * @since 1.0.0
 */
@Data
@AllArgsConstructor
public class DistanceResponse {
    private Double distanceKm;
    private Double distanceMiles;
}
