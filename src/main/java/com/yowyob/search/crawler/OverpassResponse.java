package com.yowyob.search.crawler;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;

/** Réponse de l'API Overpass (sous-ensemble utile). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OverpassResponse(List<Element> elements) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Element(long id, Double lat, Double lon, Center center, Map<String, String> tags) {

        /** Latitude effective (node {@code lat}, ou centre des way/relation). */
        public Double latitude() {
            return lat != null ? lat : (center != null ? center.lat() : null);
        }

        /** Longitude effective. */
        public Double longitude() {
            return lon != null ? lon : (center != null ? center.lon() : null);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Center(Double lat, Double lon) {
    }
}
