package com.yowyob.search.service;

/**
 * Paramètres d'une recherche. {@code rawQuery} est la saisie brute (analysée par {@link KeywordParser}
 * pour en extraire ville/proximité). Les champs géo sont optionnels : fournis explicitement par le
 * client ({@code lat}/{@code lon}/{@code radiusKm}/{@code city}) ou déduits ({@code ipAddress} pour
 * « près de moi »).
 */
public record SearchQuery(
        String tenantId,
        String rawQuery,
        String collection,
        int page,
        int size,
        Double lat,
        Double lon,
        Double radiusKm,
        String city,
        String ipAddress) {

    /** Recherche simple sans contexte géographique. */
    public static SearchQuery of(String tenantId, String rawQuery, String collection, int page, int size) {
        return new SearchQuery(tenantId, rawQuery, collection, page, size, null, null, null, null, null);
    }
}
