package com.yowyob.search.service;

import com.yowyob.search.domain.SearchDoc;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;

/** Construit un {@link SearchDoc} à partir d'un objet source arbitraire (extraction titre + aplatissement). */
public final class DocumentMapper {

    private static final List<String> TITLE_KEYS = List.of(
            "name", "title", "label", "displayName", "fullName", "longName", "shortName",
            "designation", "reference", "number", "code", "sku", "barcode", "email", "username");

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
                Instant.now());
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
