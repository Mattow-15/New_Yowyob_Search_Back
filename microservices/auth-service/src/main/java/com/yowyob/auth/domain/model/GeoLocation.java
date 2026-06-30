package com.yowyob.auth.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Position géographique d'un utilisateur, déduite de son adresse IP au moment
 * de la connexion. Attachée au {@link AuthResult} pour être renvoyée au client.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeoLocation {
    private String ip;
    private String city;
    private String country;
    private double latitude;
    private double longitude;
}
