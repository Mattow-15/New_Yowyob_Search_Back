package com.yowyob.search.service;

import com.yowyob.search.domain.SearchDoc;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;

/** Construit un {@link SearchDoc} à partir d'un objet source arbitraire (extraction titre + aplatissement). */
public final class DocumentMapper {

    private static final List<String> TITLE_KEYS = List.of(
            "name", "title", "label", "displayName", "fullName", "longName", "shortName",
            "designation", "reference", "number", "code", "sku", "barcode", "email", "username");

    private static final List<String> LATITUDE_KEYS = List.of("latitude", "lat");
    private static final List<String> LONGITUDE_KEYS = List.of("longitude", "lon", "lng");

    private DocumentMapper() {
    }

    public static SearchDoc toDocument(String tenantId, String collection, String externalId,
            Map<String, Object> source) {
        Map<String, Object> safeSource = source == null ? Map.of() : source;
        return new SearchDoc(
                SearchDoc.documentId(tenantId, collection, externalId),
                tenantId,
                collection,
                externalId,
                extractTitle(safeSource),
                flatten(safeSource),
                safeSource,
                null, // textVector renseigné de façon asynchrone par IndexService (embeddings)
                extractLocation(safeSource),
                Instant.now());
    }

    /** Extrait un {@link GeoPoint} de la source si elle porte latitude/longitude, sinon null. */
    private static GeoPoint extractLocation(Map<String, Object> source) {
        Double lat = extractCoordinate(source, LATITUDE_KEYS);
        Double lon = extractCoordinate(source, LONGITUDE_KEYS);
        if (lat == null || lon == null) {
            return null;
        }
        if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
            return null;
        }
        return new GeoPoint(lat, lon);
    }

    private static Double extractCoordinate(Map<String, Object> source, List<String> keys) {
        for (String key : keys) {
            Object value = source.get(key);
            if (value instanceof Number number) {
                return number.doubleValue();
            }
            if (value instanceof String text && !text.isBlank()) {
                try {
                    return Double.parseDouble(text.trim());
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    /** Texte servant à générer l'embedding sémantique : titre + contenu aplati. */
    public static String embeddingText(SearchDoc doc) {
        StringJoiner joiner = new StringJoiner(" ");
        if (doc.title() != null && !doc.title().isBlank()) {
            joiner.add(doc.title());
        }
        if (doc.content() != null && !doc.content().isBlank()) {
            joiner.add(doc.content());
        }
        String text = joiner.toString().trim();
        return text.isEmpty() ? null : text;
    }

    private static String extractTitle(Map<String, Object> source) {
        for (String key : TITLE_KEYS) {
            Object value = source.get(key);
            if (value instanceof String text && !text.isBlank()) {
                return text;
            }
        }
        return null;
    }

    static String flatten(Object value) {
        StringJoiner joiner = new StringJoiner(" ");
        flattenInto(value, joiner);
        String result = joiner.toString().trim();
        return result.isEmpty() ? null : result;
    }

    private static void flattenInto(Object value, StringJoiner joiner) {
        if (value == null) {
            return;
        }
        if (value instanceof Map<?, ?> map) {
            map.values().forEach(v -> flattenInto(v, joiner));
        } else if (value instanceof Collection<?> collection) {
            collection.forEach(v -> flattenInto(v, joiner));
        } else {
            String text = value.toString();
            if (!text.isBlank() && !"null".equals(text.toLowerCase(Locale.ROOT))) {
                joiner.add(text);
            }
        }
    }
}
