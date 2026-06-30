package com.yowyob.geo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de réponse pour le calcul d'itinéraire OSRM.
 * Contient la distance (mètres), la durée (secondes) et le tracé polyline.
 *
 * @author YowYob Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteResponse {
    private double distance; // in meters
    private double duration; // in seconds
    private String polyline; // Encoded polyline or JSON string of points
}
