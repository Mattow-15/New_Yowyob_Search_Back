package com.yowyob.search.crawler;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

@Component
@ConditionalOnProperty(prefix = "crawler", name = "enabled", havingValue = "true")
public class GooglePlacesClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GooglePlacesClient.class);
    private final CrawlerProperties properties;
    private final WebClient webClient;

    public GooglePlacesClient(CrawlerProperties properties, WebClient.Builder builder) {
        this.properties = properties;
        this.webClient = builder.build();
    }

    public Flux<Map<String, Object>> fetch(String type, CrawlerProperties.City city) {
        if (properties.googlePlacesApiKey().isBlank()) {
            return Flux.empty();
        }
        return webClient.get()
                .uri(builder -> builder
                        .scheme("https").host("maps.googleapis.com")
                        .path("/maps/api/place/nearbysearch/json")
                        .queryParam("location", city.lat() + "," + city.lng())
                        .queryParam("radius", city.radiusMeters())
                        .queryParam("type", type)
                        .queryParam("language", "fr")
                        .queryParam("key", properties.googlePlacesApiKey())
                        .build())
                .retrieve()
                .bodyToMono(PlacesResponse.class)
                .timeout(Duration.ofSeconds(30))
                .flatMapMany(response -> "OK".equals(response.status()) && response.results() != null
                        ? Flux.fromIterable(response.results()) : Flux.empty())
                .filter(place -> place.placeId() != null && place.name() != null
                        && place.geometry() != null && place.geometry().location() != null)
                .map(place -> toDocument(place, city.name(), type))
                .onErrorResume(error -> {
                    LOGGER.warn("Google Places unavailable (type={}): {}", type,
                            error.getClass().getSimpleName());
                    return Flux.empty();
                });
    }

    private Map<String, Object> toDocument(Place place, String city, String type) {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("id", "google_" + place.placeId());
        doc.put("name", place.name());
        doc.put("address", place.vicinity());
        doc.put("city", city);
        doc.put("category", place.types() == null || place.types().isEmpty() ? type : place.types().getFirst());
        doc.put("rating", place.rating());
        doc.put("reviewsCount", place.userRatingsTotal());
        doc.put("latitude", place.geometry().location().lat());
        doc.put("longitude", place.geometry().location().lng());
        doc.put("source", "google_places");
        doc.put("crawledAt", Instant.now().toString());
        doc.values().removeIf(value -> value == null);
        return doc;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PlacesResponse(String status, List<Place> results) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Place(@JsonProperty("place_id") String placeId, String name, String vicinity,
            Double rating, @JsonProperty("user_ratings_total") Integer userRatingsTotal,
            Geometry geometry, List<String> types) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Geometry(Location location) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Location(double lat, double lng) {}
}
