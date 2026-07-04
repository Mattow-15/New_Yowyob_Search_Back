package com.yowyob.search.crawler;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration du crawler OpenStreetMap (optionnel, activé par {@code crawler.enabled}).
 *
 * <p>Le crawler peuple yowyob-search avec des points d'intérêt (commerces, services) récupérés via
 * l'API Overpass, en indexant <b>directement</b> via {@code IndexService} (pas de saut HTTP).
 *
 * @param enabled     active le crawler (controller manuel + scheduler).
 * @param overpassUrl URL de l'API Overpass.
 * @param userAgent   User-Agent envoyé à Overpass (requis par leur politique).
 * @param scheduleCron cron du crawl automatique (vide = pas de crawl planifié).
 * @param tenantId    tenant sous lequel les documents crawlés sont indexés.
 * @param collection  collection cible (type logique) des documents crawlés.
 * @param cities      villes à parcourir (centre + rayon).
 * @param osmTypes    catégories OSM à récupérer (valeurs des clés {@code amenity}/{@code shop}…).
 */
@ConfigurationProperties(prefix = "crawler")
public record CrawlerProperties(
        boolean enabled,
        String overpassUrl,
        String userAgent,
        String scheduleCron,
        String tenantId,
        String collection,
        List<City> cities,
        List<String> osmTypes,
        String googlePlacesApiKey) {

    public CrawlerProperties {
        if (overpassUrl == null || overpassUrl.isBlank()) {
            overpassUrl = "https://overpass-api.de/api/interpreter";
        }
        if (userAgent == null || userAgent.isBlank()) {
            userAgent = "yowyob-crawler/1.0";
        }
        if (collection == null || collection.isBlank()) {
            collection = "places";
        }
        if (cities == null) {
            cities = List.of();
        }
        if (osmTypes == null || osmTypes.isEmpty()) {
            osmTypes = List.of("restaurant", "pharmacy", "bank", "fuel", "hospital", "hotel");
        }
        googlePlacesApiKey = googlePlacesApiKey == null ? "" : googlePlacesApiKey.trim();
    }

    public record City(String name, double lat, double lng, int radiusMeters) {
    }
}
