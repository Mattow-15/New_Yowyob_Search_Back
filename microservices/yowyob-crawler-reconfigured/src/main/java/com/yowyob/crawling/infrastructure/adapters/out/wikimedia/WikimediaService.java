package com.yowyob.crawling.infrastructure.adapters.out.wikimedia;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yowyob.crawling.domain.model.RawService;
import com.yowyob.crawling.application.ports.out.PhotoProviderPort;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class WikimediaService implements PhotoProviderPort {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final String WIKIMEDIA_URL = "https://commons.wikimedia.org/w/api.php";

    @Override
    public Optional<String> findPhotoUrl(RawService service) {
        if (service == null || service.getName() == null) {
            return Optional.empty();
        }
        double lat = service.getLocation() != null ? service.getLocation().latitude() : 0.0;
        double lng = service.getLocation() != null ? service.getLocation().longitude() : 0.0;

        String photoUrl = findPhoto(service.getName(), lat, lng);
        return Optional.ofNullable(photoUrl);
    }

    private String findPhoto(String name, double lat, double lng) {
        String photoUrl = searchByName(name);
        if (photoUrl != null) {
            log.debug("Photo trouvée par nom pour : {}", name);
            return photoUrl;
        }

        if (lat != 0.0 && lng != 0.0) {
            photoUrl = searchByGps(lat, lng);
            if (photoUrl != null) {
                log.debug("Photo trouvée par GPS pour : {}", name);
                return photoUrl;
            }
        }

        log.debug("Aucune photo trouvée pour : {}", name);
        return null;
    }

    private String searchByName(String name) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(WIKIMEDIA_URL)
                .queryParam("action", "query")
                .queryParam("list", "search")
                .queryParam("srsearch", name)
                .queryParam("srnamespace", "6") // fichiers/images
                .queryParam("srlimit", "1")
                .queryParam("format", "json")
                .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "YowYob-Crawler/1.0");
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);
            WikimediaResponse parsed = objectMapper.readValue(response.getBody(), WikimediaResponse.class);

            if (parsed.getQuery() == null || parsed.getQuery().getSearch() == null || parsed.getQuery().getSearch().isEmpty()) {
                return null;
            }

            String title = parsed.getQuery().getSearch().get(0).getTitle();
            return fetchImageUrl(title);

        } catch (Exception e) {
            log.warn("Erreur recherche Wikimedia par nom '{}' : {}", name, e.getMessage());
            return null;
        }
    }

    private String searchByGps(double lat, double lng) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(WIKIMEDIA_URL)
                .queryParam("action", "query")
                .queryParam("generator", "geosearch")
                .queryParam("ggscoord", lat + "|" + lng)
                .queryParam("ggsradius", "100") // rayon 100 mètres
                .queryParam("ggslimit", "1")
                .queryParam("prop", "imageinfo")
                .queryParam("iiprop", "url")
                .queryParam("format", "json")
                .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "YowYob-Crawler/1.0");
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);
            WikimediaResponse parsed = objectMapper.readValue(response.getBody(), WikimediaResponse.class);

            if (parsed.getQuery() == null || parsed.getQuery().getPages() == null || parsed.getQuery().getPages().isEmpty()) {
                return null;
            }

            return parsed.getQuery().getPages().values().stream()
                .filter(p -> p.getImageinfo() != null && !p.getImageinfo().isEmpty())
                .map(p -> p.getImageinfo().get(0).getUrl())
                .filter(this::isValidImageUrl)
                .findFirst()
                .orElse(null);

        } catch (Exception e) {
            log.warn("Erreur recherche Wikimedia par GPS ({},{}) : {}", lat, lng, e.getMessage());
            return null;
        }
    }

    private String fetchImageUrl(String title) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(WIKIMEDIA_URL)
                .queryParam("action", "query")
                .queryParam("titles", title)
                .queryParam("prop", "imageinfo")
                .queryParam("iiprop", "url")
                .queryParam("format", "json")
                .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "YowYob-Crawler/1.0");
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);
            WikimediaResponse parsed = objectMapper.readValue(response.getBody(), WikimediaResponse.class);

            if (parsed.getQuery() == null || parsed.getQuery().getPages() == null) {
                return null;
            }

            return parsed.getQuery().getPages().values().stream()
                .filter(p -> p.getImageinfo() != null && !p.getImageinfo().isEmpty())
                .map(p -> p.getImageinfo().get(0).getUrl())
                .filter(this::isValidImageUrl)
                .findFirst()
                .orElse(null);

        } catch (Exception e) {
            log.warn("Erreur fetchImageUrl pour '{}' : {}", title, e.getMessage());
            return null;
        }
    }

    private boolean isValidImageUrl(String url) {
        if (url == null) return false;
        String lower = url.toLowerCase();
        return lower.endsWith(".jpg")
            || lower.endsWith(".jpeg")
            || lower.endsWith(".png")
            || lower.endsWith(".webp");
    }

    // ── DTOs Wikimedia Internes ───────────────────────────────────

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WikimediaResponse {
        @JsonProperty("query")
        private Query query;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Query {
        @JsonProperty("pages")
        private Map<String, Page> pages;

        @JsonProperty("search")
        private List<SearchResult> search;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Page {
        @JsonProperty("title")
        private String title;

        @JsonProperty("imageinfo")
        private List<ImageInfo> imageinfo;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ImageInfo {
        @JsonProperty("url")
        private String url;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SearchResult {
        @JsonProperty("title")
        private String title;
    }
}
