package com.yowyob.crawling.infrastructure.adapters.out.googleplaces;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yowyob.crawling.domain.model.GeoPoint;
import com.yowyob.crawling.domain.model.RawService;
import com.yowyob.crawling.domain.model.ServiceSource;
import com.yowyob.crawling.application.ports.out.ServiceSourcePort;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PlacesSearchService implements ServiceSourcePort {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${google.places.api-key:}")
    private String apiKey;

    private static final String BASE_URL = "https://maps.googleapis.com/maps/api/place";

    @Override
    public ServiceSource getSourceType() {
        return ServiceSource.GOOGLE;
    }

    @Override
    public boolean isAvailable() {
        if ("true".equalsIgnoreCase(System.getenv("SCRAPER_GOOGLELOCALMOCK_ENABLED"))
                || "true".equalsIgnoreCase(System.getProperty("scraper.googlelocalmock.enabled"))) {
            return true;
        }
        return apiKey != null 
            && !apiKey.isBlank() 
            && !apiKey.equals("YOUR_GOOGLE_PLACES_API_KEY_HERE");
    }

    @Override
    public List<RawService> fetch(String cityName, double lat, double lng, int radiusMeters, String placeType) {
        if ("true".equalsIgnoreCase(System.getenv("SCRAPER_GOOGLELOCALMOCK_ENABLED"))
                || "true".equalsIgnoreCase(System.getProperty("scraper.googlelocalmock.enabled"))) {
            log.info("GoogleLocalMockScraperStrategy activée - Renvoi de données Mock");
            List<RawService> mockList = new java.util.ArrayList<>();
            mockList.add(RawService.builder()
                .rawId("google_mock_tchokos_sarl")
                .name("Tchokos SARL")
                .address("Avenue Kennedy, Yaoundé")
                .street("Avenue Kennedy")
                .city(cityName)
                .location(new GeoPoint(3.8480, 11.5021))
                .phone("6 91 98 10 47")
                .category("restaurant")
                .rating(3.7)
                .reviewCount(15)
                .reviewsSummary("Super resto || Très bon service ||")
                .openingHours("Lundi-Vendredi: 08:00 - 22:00")
                .openNow(true)
                .source(ServiceSource.GOOGLE)
                .build());
            mockList.add(RawService.builder()
                .rawId("google_mock_tchokos_express")
                .name("TCHOKOS SERVICE EXPRESS")
                .address("Rue Nachtigal, Yaoundé")
                .street("Rue Nachtigal")
                .city(cityName)
                .location(new GeoPoint(3.8490, 11.5031))
                .phone("6 91 98 10 47")
                .category("restaurant")
                .rating(5.0)
                .reviewCount(5)
                .reviewsSummary("Excellent || Rapide ||")
                .openingHours("24h/24")
                .openNow(true)
                .source(ServiceSource.GOOGLE)
                .build());
            return mockList;
        }

        List<RawService> results = new ArrayList<>();
        String pageToken = null;

        do {
            String url = buildNearbyUrl(placeType, lat, lng, radiusMeters, pageToken);

            try {
                ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
                PlacesResponse parsed = objectMapper.readValue(response.getBody(), PlacesResponse.class);

                if (parsed == null || parsed.getResults() == null) break;

                if (!"OK".equals(parsed.getStatus()) && !"ZERO_RESULTS".equals(parsed.getStatus())) {
                    log.warn("Google Places status: {}", parsed.getStatus());
                    break;
                }

                for (PlaceResult place : parsed.getResults()) {
                    PlaceDetails details = getPlaceDetails(place.getPlaceId());
                    RawService raw = toRawService(place, details, cityName);
                    results.add(raw);
                }

                pageToken = parsed.getNextPageToken();
                if (pageToken != null) {
                    sleep(2000); // Google impose un délai
                }
            } catch (Exception e) {
                log.error("Erreur Google Places fetch ({}): {}", placeType, e.getMessage());
                break;
            }
        } while (pageToken != null);

        return results;
    }

    public PlaceDetails getPlaceDetails(String placeId) {
        if (placeId == null) return null;

        String url = UriComponentsBuilder
            .fromHttpUrl(BASE_URL + "/details/json")
            .queryParam("place_id", placeId)
            .queryParam("fields", String.join(",",
                "name", "formatted_address", "formatted_phone_number",
                "website", "opening_hours", "rating", "user_ratings_total",
                "reviews", "photos", "price_level", "geometry",
                "address_components", "url", "types"))
            .queryParam("language", "fr")
            .queryParam("key", apiKey)
            .toUriString();

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            PlaceDetailsResponse detailsResponse = objectMapper.readValue(response.getBody(), PlaceDetailsResponse.class);
            return detailsResponse != null ? detailsResponse.getResult() : null;
        } catch (Exception e) {
            log.warn("Erreur détails place_id={}: {}", placeId, e.getMessage());
            return null;
        }
    }

    public String getPhotoUrl(String photoReference) {
        if (photoReference == null) return null;
        return UriComponentsBuilder
            .fromHttpUrl(BASE_URL + "/photo")
            .queryParam("maxwidth", 800)
            .queryParam("photo_reference", photoReference)
            .queryParam("key", apiKey)
            .toUriString();
    }

    private RawService toRawService(PlaceResult place, PlaceDetails details, String cityName) {
        String openingHours = null;
        Boolean openNow = null;
        if (details != null && details.getOpeningHours() != null) {
            openingHours = details.getOpeningHours().getWeekdayText() != null
                ? String.join(" | ", details.getOpeningHours().getWeekdayText())
                : null;
            openNow = details.getOpeningHours().isOpenNow();
        }

        String reviewsSummary = null;
        if (details != null && details.getReviews() != null && !details.getReviews().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            details.getReviews().stream().limit(3).forEach(r ->
                sb.append(r.getAuthorName())
                  .append(" (").append(r.getRating()).append("★): ")
                  .append(r.getText()).append(" || ")
            );
            reviewsSummary = sb.toString();
        }

        String street = extractStreet(details);
        String category = place.getTypes() != null && !place.getTypes().isEmpty()
            ? place.getTypes().get(0) : "establishment";

        double latitude = place.getGeometry() != null ? place.getGeometry().getLocation().getLat() : 0.0;
        double longitude = place.getGeometry() != null ? place.getGeometry().getLocation().getLng() : 0.0;

        return RawService.builder()
            .rawId("google_" + place.getPlaceId())
            .name(place.getName())
            .address(place.getFormattedAddress() != null
                ? place.getFormattedAddress()
                : (details != null ? details.getFormattedAddress() : null))
            .street(street)
            .city(cityName)
            .location(new GeoPoint(latitude, longitude))
            .phone(details != null ? details.getFormattedPhoneNumber() : null)
            .website(details != null ? details.getWebsite() : null)
            .category(category)
            .rating(place.getRating())
            .reviewCount(place.getUserRatingsTotal())
            .reviewsSummary(reviewsSummary)
            .openingHours(openingHours)
            .openNow(openNow)
            .priceLevel(place.getPriceLevel())
            .googleMapsUrl(details != null ? details.getUrl() : null)
            .source(ServiceSource.GOOGLE)
            .build();
    }

    private String extractStreet(PlaceDetails details) {
        if (details == null || details.getAddressComponents() == null) return null;
        return details.getAddressComponents().stream()
            .filter(c -> c.getTypes() != null && c.getTypes().contains("route"))
            .map(AddressComponent::getLongName)
            .findFirst()
            .orElse(null);
    }

    private String buildNearbyUrl(String type, double lat, double lng, int radius, String pageToken) {
        UriComponentsBuilder builder = UriComponentsBuilder
            .fromHttpUrl(BASE_URL + "/nearbysearch/json")
            .queryParam("location", lat + "," + lng)
            .queryParam("radius", radius)
            .queryParam("type", type)
            .queryParam("language", "fr")
            .queryParam("key", apiKey);

        if (pageToken != null) {
            builder.queryParam("pagetoken", pageToken);
        }
        return builder.toUriString();
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    // ── DTOs internes ─────────────────────────────────────────────

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PlacesResponse {
        private List<PlaceResult> results;
        private String status;
        private String nextPageToken;
        @JsonProperty("next_page_token")
        public void setNextPageToken(String t) { this.nextPageToken = t; }
    }

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PlaceResult {
        private String placeId;
        private String name;
        private String formattedAddress;
        private Double rating;
        private Integer userRatingsTotal;
        private Integer priceLevel;
        private Geometry geometry;
        private List<String> types;
        private List<Photo> photos;

        @JsonProperty("place_id")
        public void setPlaceId(String id) { this.placeId = id; }
        @JsonProperty("formatted_address")
        public void setFormattedAddress(String a) { this.formattedAddress = a; }
        @JsonProperty("user_ratings_total")
        public void setUserRatingsTotal(Integer u) { this.userRatingsTotal = u; }
        @JsonProperty("price_level")
        public void setPriceLevel(Integer p) { this.priceLevel = p; }
    }

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PlaceDetailsResponse {
        private PlaceDetails result;
        private String status;
    }

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PlaceDetails {
        private String name;
        private String formattedAddress;
        private String formattedPhoneNumber;
        private String website;
        private String url;
        private Double rating;
        private Integer userRatingsTotal;
        private OpeningHours openingHours;
        private List<Review> reviews;
        private List<Photo> photos;
        private List<AddressComponent> addressComponents;

        @JsonProperty("formatted_address")
        public void setFormattedAddress(String a) { this.formattedAddress = a; }
        @JsonProperty("formatted_phone_number")
        public void setFormattedPhoneNumber(String p) { this.formattedPhoneNumber = p; }
        @JsonProperty("opening_hours")
        public void setOpeningHours(OpeningHours o) { this.openingHours = o; }
        @JsonProperty("user_ratings_total")
        public void setUserRatingsTotal(Integer u) { this.userRatingsTotal = u; }
        @JsonProperty("address_components")
        public void setAddressComponents(List<AddressComponent> a) { this.addressComponents = a; }
    }

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OpeningHours {
        private boolean openNow;
        private List<String> weekdayText;

        @JsonProperty("open_now")
        public void setOpenNow(boolean o) { this.openNow = o; }
        @JsonProperty("weekday_text")
        public void setWeekdayText(List<String> w) { this.weekdayText = w; }
    }

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Review {
        private String authorName;
        private Integer rating;
        private String text;

        @JsonProperty("author_name")
        public void setAuthorName(String a) { this.authorName = a; }
    }

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Photo {
        private String photoReference;
        @JsonProperty("photo_reference")
        public void setPhotoReference(String p) { this.photoReference = p; }
    }

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Geometry {
        private Location location;
    }

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Location {
        private double lat;
        private double lng;
    }

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AddressComponent {
        private String longName;
        private List<String> types;
        @JsonProperty("long_name")
        public void setLongName(String l) { this.longName = l; }
    }
}
