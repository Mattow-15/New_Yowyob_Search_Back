package com.yowyob.crawling.infrastructure.adapters.out.osm;

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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class OsmCrawlerService implements ServiceSourcePort {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final String OVERPASS_URL = "https://overpass-api.de/api/interpreter";
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 10_000;
    private static final long REQUEST_DELAY_MS = 3_000;

    @Override
    public ServiceSource getSourceType() {
        return ServiceSource.OSM;
    }

    @Override
    public boolean isAvailable() {
        return true; // Toujours disponible car gratuit et sans API Key
    }

    @Override
    public List<RawService> fetch(String cityName, double lat, double lng, int radiusMeters, String placeType) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                log.info("Overpass query: type={} tentative={}/{}", placeType, attempt, MAX_RETRIES);
                List<OsmElement> elements = doFetch(placeType, lat, lng, radiusMeters);

                // Pause obligatoire après requête réussie
                sleep(REQUEST_DELAY_MS);

                return elements.stream()
                    .map(e -> toRawService(e, cityName))
                    .collect(Collectors.toList());

            } catch (HttpClientErrorException e) {
                if (e.getStatusCode().value() == 429) {
                    log.warn("Rate limit Overpass (429) — attente {}s avant retry {}/{}",
                        RETRY_DELAY_MS / 1000, attempt, MAX_RETRIES);
                    sleep(RETRY_DELAY_MS * attempt);
                } else {
                    log.error("Erreur HTTP Overpass {} : {}", e.getStatusCode(), e.getMessage());
                    return Collections.emptyList();
                }
            } catch (Exception e) {
                log.error("Erreur Overpass tentative {}/{} : {}", attempt, MAX_RETRIES, e.getMessage());
                if (attempt < MAX_RETRIES) {
                    sleep(RETRY_DELAY_MS);
                }
            }
        }
        log.error("Overpass API inaccessible après {} tentatives pour type={}", MAX_RETRIES, placeType);
        return Collections.emptyList();
    }

    private List<OsmElement> doFetch(String type, double lat, double lng, int radius) throws Exception {
        String query = buildOverpassQuery(type, lat, lng, radius);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("User-Agent", "YowYob-Crawler/1.0 (projet academique)");

        HttpEntity<String> request = new HttpEntity<>(
            "data=" + URLEncoder.encode(query, StandardCharsets.UTF_8),
            headers
        );

        ResponseEntity<String> response = restTemplate.postForEntity(OVERPASS_URL, request, String.class);
        OverpassResponse parsed = objectMapper.readValue(response.getBody(), OverpassResponse.class);

        List<OsmElement> elements = parsed.getElements() != null ? parsed.getElements() : List.of();

        return elements.stream()
            .filter(e -> e.getLatitude() != null && e.getLongitude() != null)
            .filter(e -> e.getTags() != null && e.getTags().containsKey("name"))
            .collect(Collectors.toList());
    }

    private RawService toRawService(OsmElement element, String city) {
        Map<String, String> tags = element.getTags();

        String address = buildAddress(tags);
        String phone = tags.getOrDefault("phone", tags.getOrDefault("contact:phone", null));
        String website = tags.getOrDefault("website", tags.getOrDefault("contact:website", null));
        String category = tags.getOrDefault("amenity", tags.getOrDefault("shop", tags.getOrDefault("tourism", "unknown")));

        return RawService.builder()
            .rawId("osm_" + element.getId())
            .name(tags.get("name"))
            .address(address)
            .street(tags.get("addr:street"))
            .city(city)
            .location(new GeoPoint(element.getLatitude(), element.getLongitude()))
            .phone(phone)
            .website(website)
            .category(category)
            .openingHours(tags.getOrDefault("opening_hours", null))
            .source(ServiceSource.OSM)
            .build();
    }

    private String buildOverpassQuery(String type, double lat, double lng, int radius) {
        return String.format("""
            [out:json][timeout:30];
            (
              node["%s"](around:%d,%f,%f);
              way["%s"](around:%d,%f,%f);
              relation["%s"](around:%d,%f,%f);
            );
            out center tags;
            """,
            typeToOsmTag(type), radius, lat, lng,
            typeToOsmTag(type), radius, lat, lng,
            typeToOsmTag(type), radius, lat, lng
        );
    }

    private String typeToOsmTag(String type) {
        return switch (type) {
            case "shop", "supermarket", "market" -> "shop";
            default -> "amenity";
        };
    }

    private String buildAddress(Map<String, String> tags) {
        StringBuilder sb = new StringBuilder();
        if (tags.containsKey("addr:housenumber"))
            sb.append(tags.get("addr:housenumber")).append(" ");
        if (tags.containsKey("addr:street"))
            sb.append(tags.get("addr:street")).append(", ");
        if (tags.containsKey("addr:city"))
            sb.append(tags.get("addr:city"));
        String result = sb.toString().trim().replaceAll(", $", "");
        return result.isEmpty() ? null : result;
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    // ── DTOs internes ─────────────────────────────────────────────

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OverpassResponse {
        private List<OsmElement> elements;
    }

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OsmElement {
        private String type;
        private Long id;
        private Double lat;
        private Double lon;
        private Double centerLat;
        private Double centerLon;
        private Map<String, String> tags;

        @JsonProperty("lat")
        public Double getLatitude() {
            return lat != null ? lat : centerLat;
        }

        @JsonProperty("lon")
        public Double getLongitude() {
            return lon != null ? lon : centerLon;
        }

        @JsonProperty("center")
        public void setCenter(Center center) {
            if (center != null) {
                this.centerLat = center.getLat();
                this.centerLon = center.getLon();
            }
        }
    }

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Center {
        private Double lat;
        private Double lon;
    }
}
