package com.yowyob.search.crawler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;
import java.time.Duration;

/**
 * Récupère des points d'intérêt via l'API Overpass (OpenStreetMap), autour d'un centre et d'un rayon.
 * Tolérant aux pannes : un échec (timeout, 429…) renvoie un flux vide après quelques essais.
 */
@Component
@ConditionalOnProperty(prefix = "crawler", name = "enabled", havingValue = "true")
public class OverpassClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(OverpassClient.class);

    private final CrawlerProperties properties;
    private final WebClient webClient;

    public OverpassClient(CrawlerProperties properties, WebClient.Builder builder) {
        this.properties = properties;
        this.webClient = builder
                .codecs(c -> c.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build();
    }

    /** Récupère les éléments OSM d'un type donné autour d'un point. */
    public Flux<OverpassResponse.Element> fetch(String osmType, double lat, double lng, int radiusMeters) {
        String query = buildQuery(osmType, lat, lng, radiusMeters);
        return webClient.post()
                .uri(properties.overpassUrl())
                .header("User-Agent", properties.userAgent())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("data", query))
                .retrieve()
                .bodyToMono(OverpassResponse.class)
                .timeout(Duration.ofSeconds(40))
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(5)))
                .flatMapMany(response -> response.elements() == null
                        ? Flux.empty()
                        : Flux.fromIterable(response.elements()))
                .onErrorResume(error -> {
                    LOGGER.warn("Overpass indisponible (type={}): {}", osmType, error.toString());
                    return Flux.empty();
                });
    }

    /** Clé OSM appropriée selon le type demandé (shop vs amenity vs tourism). */
    private static String osmKey(String type) {
        return switch (type) {
            case "shop", "supermarket", "market", "mall" -> "shop";
            case "hotel", "guest_house", "hostel", "attraction" -> "tourism";
            default -> "amenity";
        };
    }

    private static String buildQuery(String type, double lat, double lng, int radius) {
        String key = osmKey(type);
        return String.format("""
                [out:json][timeout:30];
                (
                  node["%s"="%s"](around:%d,%f,%f);
                  way["%s"="%s"](around:%d,%f,%f);
                  relation["%s"="%s"](around:%d,%f,%f);
                );
                out center tags;
                """,
                key, type, radius, lat, lng,
                key, type, radius, lat, lng,
                key, type, radius, lat, lng);
    }
}
