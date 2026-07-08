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
import reactor.core.publisher.Mono;

@Component
@ConditionalOnProperty(prefix = "crawler", name = "enabled", havingValue = "true")
public class GooglePlacesClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GooglePlacesClient.class);
    // Champs demandés à Place Details : website + téléphone + horaires + adresse formatée
    private static final String DETAIL_FIELDS = "website,formatted_phone_number,formatted_address,opening_hours,international_phone_number";

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
                // Appel Place Details pour chaque lieu (website, téléphone, horaires)
                .concatMap(place -> fetchDetails(place.placeId())
                        .map(details -> toDocument(place, details, city.name(), type))
                        .delaySubscription(Duration.ofMillis(100)) // politesse API
                        .onErrorReturn(toDocument(place, null, city.name(), type)))
                .onErrorResume(error -> {
                    LOGGER.warn("Google Places unavailable (type={}): {}", type,
                            error.getClass().getSimpleName());
                    return Flux.empty();
                });
    }

    private Mono<PlaceDetails> fetchDetails(String placeId) {
        return webClient.get()
                .uri(builder -> builder
                        .scheme("https").host("maps.googleapis.com")
                        .path("/maps/api/place/details/json")
                        .queryParam("place_id", placeId)
                        .queryParam("fields", DETAIL_FIELDS)
                        .queryParam("language", "fr")
                        .queryParam("key", properties.googlePlacesApiKey())
                        .build())
                .retrieve()
                .bodyToMono(DetailsResponse.class)
                .timeout(Duration.ofSeconds(15))
                .mapNotNull(r -> "OK".equals(r.status()) ? r.result() : null)
                .onErrorResume(e -> {
                    LOGGER.debug("Place Details failed for {}: {}", placeId, e.getMessage());
                    return Mono.empty();
                });
    }

    private Map<String, Object> toDocument(Place place, PlaceDetails details, String city, String type) {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("id", "google_" + place.placeId());
        doc.put("name", place.name());
        doc.put("city", city);
        doc.put("category", place.types() == null || place.types().isEmpty() ? type : place.types().getFirst());
        doc.put("rating", place.rating());
        doc.put("reviewsCount", place.userRatingsTotal());
        doc.put("latitude", place.geometry().location().lat());
        doc.put("longitude", place.geometry().location().lng());
        doc.put("source", "google_places");
        doc.put("crawledAt", Instant.now().toString());

        if (details != null) {
            doc.put("website", details.website());
            doc.put("phone", firstNonNull(details.internationalPhoneNumber(), details.formattedPhoneNumber()));
            doc.put("address", details.formattedAddress());
            if (details.openingHours() != null && details.openingHours().weekdayText() != null) {
                doc.put("openingHours", String.join(" | ", details.openingHours().weekdayText()));
            }
        } else {
            doc.put("address", place.vicinity());
        }

        doc.values().removeIf(value -> value == null);
        return doc;
    }

    @SafeVarargs
    private static <T> T firstNonNull(T... values) {
        for (T v : values) { if (v != null) return v; }
        return null;
    }

    // ── Records de désérialisation ────────────────────────────────
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    record DetailsResponse(String status, PlaceDetails result) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PlaceDetails(
            String website,
            @JsonProperty("formatted_phone_number") String formattedPhoneNumber,
            @JsonProperty("international_phone_number") String internationalPhoneNumber,
            @JsonProperty("formatted_address") String formattedAddress,
            @JsonProperty("opening_hours") OpeningHours openingHours) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OpeningHours(@JsonProperty("weekday_text") List<String> weekdayText) {}
}
