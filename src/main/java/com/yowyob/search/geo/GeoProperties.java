package com.yowyob.search.geo;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration de la géolocalisation (géocodage d'adresse + géoloc par IP).
 *
 * @param enabled        active les fonctions géo (géocodage, recherche de proximité, /api/geo).
 * @param nominatimUrl   base URL du service de géocodage Nominatim (OpenStreetMap).
 * @param ipapiUrl       base URL du service de géoloc par IP (ipapi.co).
 * @param userAgent      User-Agent envoyé à Nominatim (requis par leur politique d'usage).
 * @param defaultLat     latitude de repli quand l'IP est locale/inconnue (Douala).
 * @param defaultLon     longitude de repli quand l'IP est locale/inconnue (Douala).
 * @param defaultRadiusKm rayon par défaut d'une recherche de proximité.
 */
@ConfigurationProperties(prefix = "yowyob.search.geo")
public record GeoProperties(
        boolean enabled,
        String nominatimUrl,
        String ipapiUrl,
        String userAgent,
        double defaultLat,
        double defaultLon,
        double defaultRadiusKm) {

    public GeoProperties {
        if (nominatimUrl == null || nominatimUrl.isBlank()) {
            nominatimUrl = "https://nominatim.openstreetmap.org";
        }
        if (ipapiUrl == null || ipapiUrl.isBlank()) {
            ipapiUrl = "https://ipapi.co";
        }
        if (userAgent == null || userAgent.isBlank()) {
            userAgent = "yowyob-search";
        }
        if (defaultLat == 0) {
            defaultLat = 4.0511; // Douala
        }
        if (defaultLon == 0) {
            defaultLon = 9.7679; // Douala
        }
        if (defaultRadiusKm <= 0) {
            defaultRadiusKm = 10.0;
        }
    }
}
