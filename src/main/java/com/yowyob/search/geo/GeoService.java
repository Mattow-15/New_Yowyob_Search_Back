package com.yowyob.search.geo;

import com.yowyob.search.geo.GeoDtos.DistanceResult;
import com.yowyob.search.geo.GeoDtos.GeocodeResult;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Géocodage d'adresse → coordonnées + calcul de distance (Haversine).
 *
 * <p>Stratégie sans Redis ni base : un <b>gazetteer intégré</b> des principales villes camerounaises
 * répond instantanément et hors-ligne ; pour le reste on interroge Nominatim (OSM). Les résultats
 * sont mis en cache mémoire. Tolérant aux pannes : un géocodage qui échoue renvoie {@link Mono#empty()}.
 */
@Service
@EnableConfigurationProperties(GeoProperties.class)
public class GeoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeoService.class);
    private static final int EARTH_RADIUS_KM = 6371;

    /** Villes camerounaises connues → [lat, lon]. Réponse instantanée, sans appel réseau. */
    private static final Map<String, double[]> GAZETTEER = Map.ofEntries(
            Map.entry("douala", new double[] {4.0511, 9.7679}),
            Map.entry("yaounde", new double[] {3.8480, 11.5021}),
            Map.entry("bafoussam", new double[] {5.4768, 10.4214}),
            Map.entry("bamenda", new double[] {5.9597, 10.1459}),
            Map.entry("garoua", new double[] {9.3017, 13.3921}),
            Map.entry("maroua", new double[] {10.5910, 14.3158}),
            Map.entry("ngaoundere", new double[] {7.3167, 13.5833}),
            Map.entry("bertoua", new double[] {4.5772, 13.6846}),
            Map.entry("buea", new double[] {4.1537, 9.2920}),
            Map.entry("limbe", new double[] {4.0186, 9.2147}),
            Map.entry("kribi", new double[] {2.9370, 9.9100}),
            Map.entry("ebolowa", new double[] {2.9000, 11.1500}),
            Map.entry("dschang", new double[] {5.4500, 10.0667}),
            Map.entry("kumba", new double[] {4.6363, 9.4469}),
            Map.entry("edea", new double[] {3.8000, 10.1333}),
            Map.entry("nkongsamba", new double[] {4.9547, 9.9404}),
            Map.entry("foumban", new double[] {5.7167, 10.9000}),
            Map.entry("tiko", new double[] {4.0750, 9.3600}));

    private final GeoProperties properties;
    private final WebClient webClient;
    private final Map<String, GeocodeResult> cache = new ConcurrentHashMap<>();

    public GeoService(GeoProperties properties, WebClient.Builder builder) {
        this.properties = properties;
        this.webClient = builder.build();
    }

    /** Géocode une adresse en coordonnées (gazetteer d'abord, puis Nominatim, avec cache). */
    public Mono<GeocodeResult> geocode(String address) {
        if (address == null || address.isBlank()) {
            return Mono.empty();
        }
        String key = normalize(address);
        GeocodeResult cached = cache.get(key);
        if (cached != null) {
            return Mono.just(cached);
        }
        double[] known = GAZETTEER.get(key);
        if (known != null) {
            GeocodeResult result = new GeocodeResult(address, known[0], known[1]);
            cache.put(key, result);
            return Mono.just(result);
        }
        return fetchFromNominatim(address)
                .doOnNext(result -> cache.put(key, result));
    }

    @SuppressWarnings("unchecked")
    private Mono<GeocodeResult> fetchFromNominatim(String address) {
        return webClient.get()
                .uri(properties.nominatimUrl() + "/search?format=json&limit=1&q={q}", address)
                .header("User-Agent", properties.userAgent())
                .retrieve()
                .bodyToMono(List.class)
                .flatMap(list -> {
                    if (list.isEmpty()) {
                        return Mono.empty();
                    }
                    Map<String, Object> first = (Map<String, Object>) list.get(0);
                    return Mono.just(new GeocodeResult(
                            String.valueOf(first.get("display_name")),
                            Double.parseDouble(String.valueOf(first.get("lat"))),
                            Double.parseDouble(String.valueOf(first.get("lon")))));
                })
                .onErrorResume(error -> {
                    LOGGER.warn("Géocodage Nominatim indisponible pour '{}': {}", address, error.toString());
                    return Mono.empty();
                });
    }

    /** Distance grand-cercle (Haversine) entre deux points. */
    public DistanceResult calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double km = haversine(lat1, lon1, lat2, lon2);
        return new DistanceResult(km, km * 0.621371);
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private static String normalize(String input) {
        String n = Normalizer.normalize(input.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return n.replaceAll("\\s+", " ").trim();
    }
}
