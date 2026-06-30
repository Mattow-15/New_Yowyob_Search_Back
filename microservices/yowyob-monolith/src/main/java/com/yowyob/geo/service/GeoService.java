/**
 * Geolocation service providing geocoding, distance calculation, and routing.
 * Uses Nominatim for geocoding, OSRM for routing, and Redis for caching.
 * @author Matteo Owona, Rouchda Yampen
 * @date 2024-01-14
 * @updated 2025-02-11
 */
package com.yowyob.geo.service;

import com.yowyob.geo.dto.DistanceResponse;
import com.yowyob.geo.dto.GeocodeResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import com.yowyob.geo.dto.RouteResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;

@Service
@Slf4j
public class GeoService {

    private final WebClient webClient;
    private final ReactiveRedisTemplate<String, GeocodeResponse> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.nominatim.url}")
    private String nominatimUrl;

    @Value("${app.nominatim.user-agent}")
    private String userAgent;

    private static final String OSRM_BASE_URL = "http://router.project-osrm.org/route/v1";

    public GeoService(WebClient webClient,
            @Qualifier("reactiveRedisTemplateForGeocode") ReactiveRedisTemplate<String, GeocodeResponse> redisTemplate,
            ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Geocodes an address string to geographic coordinates.
     * Results are cached in Redis for 30 days.
     *
     * @param address the address to geocode
     * @return the geocode response with lat/lon coordinates
     */
    public Mono<GeocodeResponse> geocode(String address) {
        String cache_key = "geo:geocode:" + address.toLowerCase().trim().replaceAll("\\s+", "_");

        return redisTemplate.opsForValue().get(cache_key)
                .switchIfEmpty(fetchFromNominatim(address)
                        .flatMap(response -> redisTemplate.opsForValue().set(cache_key, response, Duration.ofDays(30))
                                .thenReturn(response)));
    }

    /**
     * Fetches coordinates from Nominatim geocoding API.
     *
     * @param address the address to geocode
     * @return the geocode response
     */
    private Mono<GeocodeResponse> fetchFromNominatim(String address) {
        log.info("Fetching coordinates for address: {}", address);
        return webClient.get()
                .uri(nominatimUrl + "/search?format=json&q=" + address + "&limit=1")
                .header("User-Agent", userAgent)
                .retrieve()
                .bodyToMono(List.class)
                .flatMap(list -> {
                    if (list.isEmpty()) {
                        return Mono.empty();
                    }
                    Map<String, Object> result = (Map<String, Object>) list.get(0);
                    GeocodeResponse response = GeocodeResponse.builder()
                            .address((String) result.get("display_name"))
                            .latitude(Double.parseDouble((String) result.get("lat")))
                            .longitude(Double.parseDouble((String) result.get("lon")))
                            .build();
                    return Mono.just(response);
                })
                .onErrorResume(e -> {
                    log.error("Error calling Nominatim: {}", e.getMessage());
                    return Mono.empty();
                });
    }

    /**
     * Calculates the distance between two geographic points using the Haversine
     * formula.
     *
     * @param lat1 latitude of the first point
     * @param lon1 longitude of the first point
     * @param lat2 latitude of the second point
     * @param lon2 longitude of the second point
     * @return the distance response with km and miles
     */
    public DistanceResponse calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double distance_km = haversine(lat1, lon1, lat2, lon2);
        return new DistanceResponse(distance_km, distance_km * 0.621371);
    }

    /**
     * Computes the great-circle distance between two points using the Haversine
     * formula.
     *
     * @param lat1 latitude of the first point
     * @param lon1 longitude of the first point
     * @param lat2 latitude of the second point
     * @param lon2 longitude of the second point
     * @return the distance in kilometers
     */
    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double lat_distance = Math.toRadians(lat2 - lat1);
        double lon_distance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(lat_distance / 2) * Math.sin(lat_distance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(lon_distance / 2) * Math.sin(lon_distance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * Calculates a route between two points using the OSRM service.
     * Falls back to a simple straight-line route if the API call fails.
     *
     * @param start_lat latitude of the starting point
     * @param start_lon longitude of the starting point
     * @param end_lat   latitude of the ending point
     * @param end_lon   longitude of the ending point
     * @param mode      the transport mode (driving, walking, cycling)
     * @return the route response with distance, duration, and polyline
     */
    public Mono<RouteResponse> getRoute(double start_lat, double start_lon, double end_lat, double end_lon,
            String mode) {
        String transport_mode = "driving";
        if (mode != null) {
            mode = mode.toLowerCase();
            if (mode.equals("walking") || mode.equals("bike") || mode.equals("cycling") || mode.equals("foot")) {
                if (mode.equals("bike"))
                    transport_mode = "cycling";
                else if (mode.equals("foot"))
                    transport_mode = "walking";
                else
                    transport_mode = mode;
            }
        }

        String coordinates = String.format("%f,%f;%f,%f", start_lon, start_lat, end_lon, end_lat);
        String uri = String.format("%s/%s/%s?overview=full&geometries=geojson", OSRM_BASE_URL, transport_mode,
                coordinates);

        log.info("Fetching route from OSRM: {}", uri);

        final String final_transport_mode = transport_mode;

        return webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(String.class)
                .map(json -> {
                    try {
                        JsonNode root = objectMapper.readTree(json);
                        JsonNode route = root.path("routes").get(0);

                        double distance = route.path("distance").asDouble();
                        double duration = route.path("duration").asDouble();

                        // OSRM public API often defaults to driving durations.
                        // Force manual calculation for non-driving modes based on distance (m).
                        if (!"driving".equals(final_transport_mode) && !"car".equals(final_transport_mode)) {
                            double speed_m_s = 1.4; // default ~5 km/h
                            if ("cycling".equals(final_transport_mode) || "bike".equals(final_transport_mode)) {
                                speed_m_s = 5.5; // ~20 km/h
                            }
                            duration = distance / speed_m_s;
                        }

                        JsonNode geometry = route.path("geometry").path("coordinates");

                        List<List<Double>> points = new ArrayList<>();
                        if (geometry.isArray()) {
                            for (JsonNode point : geometry) {
                                List<Double> lat_lng = new ArrayList<>();
                                lat_lng.add(point.get(1).asDouble());
                                lat_lng.add(point.get(0).asDouble());
                                points.add(lat_lng);
                            }
                        }

                        return RouteResponse.builder()
                                .distance(distance)
                                .duration(duration)
                                .polyline(objectMapper.writeValueAsString(points))
                                .build();
                    } catch (Exception e) {
                        throw new RuntimeException("Error parsing OSRM response", e);
                    }
                })
                .onErrorResume(e -> {
                    log.error("Error fetching route: {}", e.getMessage());
                    try {
                        List<List<Double>> simple_route = new ArrayList<>();
                        simple_route.add(List.of(start_lat, start_lon));
                        simple_route.add(List.of(end_lat, end_lon));

                        return Mono.just(RouteResponse.builder()
                                .distance(haversine(start_lat, start_lon, end_lat, end_lon) * 1000)
                                .duration(0)
                                .polyline(objectMapper.writeValueAsString(simple_route))
                                .build());
                    } catch (Exception ex) {
                        return Mono.empty();
                    }
                });
    }
}
