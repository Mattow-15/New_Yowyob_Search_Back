package com.yowyob.crawling.domain.model;

public record GeoPoint(double latitude, double longitude) {
    public GeoPoint {
        if (latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException("La latitude doit être comprise entre -90 et 90 degrés.");
        }
        if (longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException("La longitude doit être comprise entre -180 et 180 degrés.");
        }
    }
}
