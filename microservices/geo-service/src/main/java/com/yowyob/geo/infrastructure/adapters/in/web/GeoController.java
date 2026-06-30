package com.yowyob.geo.infrastructure.adapters.in.web;

import com.yowyob.geo.application.ports.in.CalculateRouteUseCase;
import com.yowyob.geo.application.ports.in.GeolocalizeUseCase;
import com.yowyob.geo.domain.model.Distance;
import com.yowyob.geo.infrastructure.adapters.in.web.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/geo")
@RequiredArgsConstructor
public class GeoController {

    private final GeolocalizeUseCase geolocalizeUseCase;
    private final CalculateRouteUseCase calculateRouteUseCase;

    @GetMapping("/geocode")
    public Mono<ResponseEntity<GeocodeResponse>> geocode(@RequestParam String address) {
        return geolocalizeUseCase.geocode(address)
                .map(location -> ResponseEntity.ok(GeocodeResponse.builder()
                        .address(location.getAddress())
                        .latitude(location.getLatitude())
                        .longitude(location.getLongitude())
                        .build()))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping("/distance")
    public ResponseEntity<DistanceResponse> calculateDistance(@RequestBody DistanceRequest request) {
        Distance distance = calculateRouteUseCase.calculateDistance(
                request.getLat1(), request.getLon1(),
                request.getLat2(), request.getLon2());
        DistanceResponse response = new DistanceResponse(distance.getDistanceKm(), distance.getDistanceMiles());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/ip-location")
    public Mono<ResponseEntity<GeoLocationDto>> getIpLocation(
            @RequestParam(required = false) String ip,
            @RequestHeader(value = "X-Forwarded-For", required = false) String xForwardedFor,
            @RequestHeader(value = "Remote-Addr", required = false) String remoteAddr) {

        String targetIp = ip;
        if (targetIp == null || targetIp.isEmpty()) {
            targetIp = xForwardedFor != null ? xForwardedFor.split(",")[0].trim() : remoteAddr;
        }

        // Localhost fallback
        if (targetIp == null || targetIp.equals("0:0:0:0:0:0:0:1") || targetIp.equals("127.0.0.1")) {
            targetIp = "127.0.0.1";
        }

        String finalIp = targetIp;
        return geolocalizeUseCase.getLocationFromIp(finalIp)
                .map(location -> ResponseEntity.ok(GeoLocationDto.builder()
                        .city(location.getCity())
                        .country(location.getCountry())
                        .latitude(location.getLatitude())
                        .longitude(location.getLongitude())
                        .build()))
                .doOnError(error -> System.err.println("Error getting IP location for " + finalIp + ": " + error.getMessage()));
    }

    @GetMapping("/route")
    public Mono<ResponseEntity<RouteResponse>> getRoute(
            @RequestParam double startLat, @RequestParam double startLon,
            @RequestParam double endLat, @RequestParam double endLon,
            @RequestParam(required = false, defaultValue = "driving") String mode) {
        return calculateRouteUseCase.getRoute(startLat, startLon, endLat, endLon, mode)
                .map(route -> ResponseEntity.ok(RouteResponse.builder()
                        .distance(route.getDistance())
                        .duration(route.getDuration())
                        .polyline(route.getPolyline())
                        .build()))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/health")
    public String health() {
        return "Geo Service is running!";
    }
}
