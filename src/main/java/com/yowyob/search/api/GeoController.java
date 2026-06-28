package com.yowyob.search.api;

import com.yowyob.search.geo.GeoDtos.DistanceResult;
import com.yowyob.search.geo.GeoDtos.GeocodeResult;
import com.yowyob.search.geo.GeoDtos.GeoLocation;
import com.yowyob.search.geo.GeoService;
import com.yowyob.search.geo.IpGeolocationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Services géo utilitaires (sous {@code /api/geo}, donc soumis à l'auth kernel) : géocodage d'une
 * adresse, distance entre deux points, et localisation par IP. Pratiques pour un front
 * cartographique (Leaflet / Google Maps).
 */
@RestController
@RequestMapping("/api/geo")
public class GeoController {

    private final GeoService geoService;
    private final IpGeolocationService ipGeolocationService;

    public GeoController(GeoService geoService, IpGeolocationService ipGeolocationService) {
        this.geoService = geoService;
        this.ipGeolocationService = ipGeolocationService;
    }

    @GetMapping("/geocode")
    public Mono<ResponseEntity<GeocodeResult>> geocode(@RequestParam("address") String address) {
        return geoService.geocode(address)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/distance")
    public ResponseEntity<DistanceResult> distance(
            @RequestParam double lat1, @RequestParam double lon1,
            @RequestParam double lat2, @RequestParam double lon2) {
        return ResponseEntity.ok(geoService.calculateDistance(lat1, lon1, lat2, lon2));
    }

    @GetMapping("/ip-location")
    public Mono<ResponseEntity<GeoLocation>> ipLocation(
            @RequestParam(value = "ip", required = false) String ip,
            ServerWebExchange exchange) {
        String target = (ip != null && !ip.isBlank()) ? ip : clientIp(exchange);
        return ipGeolocationService.getLocationFromIp(target).map(ResponseEntity::ok);
    }

    private static String clientIp(ServerWebExchange exchange) {
        String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return exchange.getRequest().getRemoteAddress() != null
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : null;
    }
}
