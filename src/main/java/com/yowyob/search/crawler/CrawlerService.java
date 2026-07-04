package com.yowyob.search.crawler;

import com.yowyob.search.service.IndexService;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Orchestration du crawl : pour chaque ville × type OSM, récupère les points d'intérêt via
 * {@link OverpassClient}, les transforme en documents et les indexe <b>directement</b> via
 * {@link IndexService} (sous le tenant/collection configurés). Une pause entre appels respecte la
 * politique d'usage d'Overpass.
 */
@Service
@ConditionalOnProperty(prefix = "crawler", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(CrawlerProperties.class)
public class CrawlerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrawlerService.class);
    private static final Duration POLITENESS_DELAY = Duration.ofSeconds(2);

    private final CrawlerProperties properties;
    private final OverpassClient overpassClient;
    private final GooglePlacesClient googlePlacesClient;
    private final IndexService indexService;

    public CrawlerService(CrawlerProperties properties, OverpassClient overpassClient,
            GooglePlacesClient googlePlacesClient, IndexService indexService) {
        this.properties = properties;
        this.overpassClient = overpassClient;
        this.googlePlacesClient = googlePlacesClient;
        this.indexService = indexService;
    }

    /** Lance un crawl complet et renvoie le nombre de documents indexés. */
    public Mono<Long> crawl() {
        if (properties.tenantId() == null || properties.tenantId().isBlank()) {
            return Mono.error(new IllegalStateException("crawler.tenant-id requis pour crawler."));
        }
        LOGGER.info("Crawl démarré : {} ville(s) × {} type(s) OSM", properties.cities().size(),
                properties.osmTypes().size());

        return Flux.fromIterable(properties.cities())
                .concatMap(city -> Flux.fromIterable(properties.osmTypes())
                        .concatMap(type -> crawlCityType(city, type).delaySubscription(POLITENESS_DELAY)))
                .reduce(0L, Long::sum)
                .doOnSuccess(total -> LOGGER.info("Crawl terminé : {} document(s) indexé(s).", total));
    }

    private Mono<Long> crawlCityType(CrawlerProperties.City city, String type) {
        Flux<Map<String, Object>> osmDocuments = overpassClient.fetch(type, city.lat(), city.lng(), city.radiusMeters())
                .map(element -> toDocument(element, city.name(), type))
                .filter(doc -> doc.get("name") != null && doc.get("latitude") != null);
        Flux<Map<String, Object>> documents = Flux.concat(
                osmDocuments,
                googlePlacesClient.fetch(type, city));

        return indexService.indexBulk(properties.tenantId(), properties.collection(), documents)
                .count()
                .doOnNext(count -> LOGGER.info("  {} / {} : {} indexé(s)", city.name(), type, count));
    }

    private Map<String, Object> toDocument(OverpassResponse.Element element, String city, String type) {
        Map<String, String> tags = element.tags() != null ? element.tags() : Map.of();
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("id", "osm_" + element.id());
        doc.put("name", tags.get("name"));
        doc.put("category", firstNonNull(tags.get("amenity"), tags.get("shop"), tags.get("tourism"), type));
        doc.put("address", buildAddress(tags));
        doc.put("street", tags.get("addr:street"));
        doc.put("city", city);
        doc.put("phone", firstNonNull(tags.get("phone"), tags.get("contact:phone")));
        doc.put("website", firstNonNull(tags.get("website"), tags.get("contact:website")));
        doc.put("openingHours", tags.get("opening_hours"));
        doc.put("latitude", element.latitude());
        doc.put("longitude", element.longitude());
        doc.put("source", "openstreetmap");
        doc.put("crawledAt", Instant.now().toString());
        doc.values().removeIf(value -> value == null);
        return doc;
    }

    private static String buildAddress(Map<String, String> tags) {
        StringBuilder sb = new StringBuilder();
        if (tags.containsKey("addr:housenumber")) {
            sb.append(tags.get("addr:housenumber")).append(' ');
        }
        if (tags.containsKey("addr:street")) {
            sb.append(tags.get("addr:street")).append(", ");
        }
        if (tags.containsKey("addr:city")) {
            sb.append(tags.get("addr:city"));
        }
        String address = sb.toString().trim();
        if (address.endsWith(",")) {
            address = address.substring(0, address.length() - 1);
        }
        return address.isBlank() ? null : address;
    }

    @SafeVarargs
    private static <T> T firstNonNull(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}
