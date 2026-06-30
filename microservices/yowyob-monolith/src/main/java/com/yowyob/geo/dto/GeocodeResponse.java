package com.yowyob.geo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * DTO de réponse pour le géocodage d'une adresse.
 * Contient les coordonnées GPS et l'adresse formatée.
 *
 * @author YowYob Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeocodeResponse implements Serializable {
    private String address;
    private Double latitude;
    private Double longitude;
}
