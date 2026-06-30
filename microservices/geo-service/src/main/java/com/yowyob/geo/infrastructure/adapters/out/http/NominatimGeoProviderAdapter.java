package com.yowyob.geo.infrastructure.adapters.out.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yowyob.geo.application.ports.out.GeoProviderPort;
import com.yowyob.geo.domain.model.GeocodeLocation;
import com.yowyob.geo.domain.model.Route;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class NominatimGeoProviderAdapter implements GeoProviderPort {

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.nominatim.url}")
    private String nominatimUrl;

    @Value("${app.nominatim.user-agent}")
    private String userAgent;

    private static final String OSRM_BASE_URL = "http://router.project-osrm.org/route/v1";

    public NominatimGeoProviderAdapter(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    @Override
    public Mono<GeocodeLocation> fetchFromNominatim(String address) {
        log.info("Fetching coordinates from Nominatim for address: {}", address);
        return webClient.get()
                .uri(nominatimUrl + "/search?format=json&q=" + address + "&limit=1&countrycodes=cm")
                .header("User-Agent", userAgent)
                .retrieve()
                .bodyToMono(List.class)
                .flatMap(list -> {
                    if (list.isEmpty()) {
                        return Mono.empty();
                    }
                    Map<String, Object> result = (Map<String, Object>) list.get(0);
                    GeocodeLocation location = GeocodeLocation.builder()
                            .address((String) result.get("display_name"))
                            .latitude(Double.parseDouble((String) result.get("lat")))
                            .longitude(Double.parseDouble((String) result.get("lon")))
                            .build();
                    return Mono.just(location);
                })
                .onErrorResume(e -> {
                    log.error("Error calling Nominatim: {}", e.getMessage());
                    return Mono.empty();
                });
    }

    @Override
    public Mono<Route> fetchRoute(double startLat, double startLon, double endLat, double endLon, String mode) {
        String osrmProfile;
        if (mode == null) {
            osrmProfile = "driving";
        } else {
            switch (mode.toLowerCase()) {
                case "walking": case "foot": osrmProfile = "foot"; break;
                case "cycling": case "bike": osrmProfile = "bike"; break;
                default:                     osrmProfile = "driving";
            }
        }

        String coordinates = String.format("%f,%f;%f,%f", startLon, startLat, endLon, endLat);
        String uri = String.format("%s/%s/%s?overview=full&geometries=geojson", OSRM_BASE_URL, osrmProfile, coordinates);

        log.info("Fetching route from OSRM: {}", uri);

        return webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(String.class)
                .map(json -> {
                    try {
                        JsonNode root = objectMapper.readTree(json);
                        JsonNode routeNode = root.path("routes").get(0);

                        double distance = routeNode.path("distance").asDouble();
                        double duration = routeNode.path("duration").asDouble();
                        JsonNode geometry = routeNode.path("geometry").path("coordinates");

                        List<List<Double>> points = new ArrayList<>();
                        if (geometry.isArray()) {
                            for (JsonNode point : geometry) {
                                List<Double> latLng = new ArrayList<>();
                                latLng.add(point.get(1).asDouble()); // lat
                                latLng.add(point.get(0).asDouble()); // lon
                                points.add(latLng);
                            }
                        }

                        return Route.builder()
                                .distance(distance)
                                .duration(duration)
                                .polyline(objectMapper.writeValueAsString(points))
                                .build();
                    } catch (Exception e) {
                        throw new RuntimeException("Error parsing OSRM response", e);
                    }
                });
    }
}
