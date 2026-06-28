package com.yowyob.search.geo;

import com.yowyob.search.geo.GeoDtos.GeoLocation;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Résout la localisation approximative d'un utilisateur depuis son IP (ipapi.co), avec cache mémoire.
 * Pour une IP locale/inconnue ou en cas d'erreur, renvoie une localisation par défaut (Douala) —
 * jamais d'échec dur : la recherche de proximité « près de moi » a toujours un point de départ.
 */
@Service
public class IpGeolocationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(IpGeolocationService.class);

    private final GeoProperties properties;
    private final WebClient webClient;
    private final Map<String, GeoLocation> cache = new ConcurrentHashMap<>();

    public IpGeolocationService(GeoProperties properties, WebClient.Builder builder) {
        this.properties = properties;
        this.webClient = builder.build();
    }

    public Mono<GeoLocation> getLocationFromIp(String ipAddress) {
        if (isLocalOrEmpty(ipAddress)) {
            return Mono.just(defaultLocation());
        }
        GeoLocation cached = cache.get(ipAddress);
        if (cached != null) {
            return Mono.just(cached);
        }
        return fetchFromIpApi(ipAddress)
                .doOnNext(location -> cache.put(ipAddress, location))
                .defaultIfEmpty(defaultLocation());
    }

    @SuppressWarnings("unchecked")
    private Mono<GeoLocation> fetchFromIpApi(String ipAddress) {
        return webClient.get()
                .uri(properties.ipapiUrl() + "/{ip}/json/", ipAddress)
                .header("User-Agent", properties.userAgent())
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> new GeoLocation(
                        parseDouble(response.get("latitude")),
                        parseDouble(response.get("longitude")),
                        (String) response.get("city"),
                        (String) response.get("country_name")))
                .onErrorResume(error -> {
                    LOGGER.warn("Géoloc IP indisponible pour {}: {}", ipAddress, error.toString());
                    return Mono.just(defaultLocation());
                });
    }

    private boolean isLocalOrEmpty(String ip) {
        return ip == null || ip.isBlank() || ip.equals("127.0.0.1") || ip.equals("0:0:0:0:0:0:0:1")
                || ip.equals("::1");
    }

    private GeoLocation defaultLocation() {
        return new GeoLocation(properties.defaultLat(), properties.defaultLon(), "Douala", "Cameroon");
    }

    private static Double parseDouble(Object value) {
        return value == null ? null : Double.parseDouble(value.toString());
    }
}
