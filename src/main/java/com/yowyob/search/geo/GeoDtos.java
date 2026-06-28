package com.yowyob.search.geo;

/** DTO géo (géocodage, distance, géoloc IP) — regroupés pour limiter la surface de fichiers. */
public final class GeoDtos {

    private GeoDtos() {
    }

    /** Coordonnées d'une adresse géocodée. */
    public record GeocodeResult(String address, double latitude, double longitude) {
    }

    /** Distance entre deux points. */
    public record DistanceResult(double distanceKm, double distanceMiles) {
    }

    /** Localisation déduite d'une IP (ou repli par défaut). */
    public record GeoLocation(Double latitude, Double longitude, String city, String country) {
    }
}
