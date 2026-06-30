/**
 * REST controller for geolocation endpoints.
 * Provides geocoding, distance calculation, IP-based location, and routing.
 * @author Matteo Owona, Rouchda Yampen
 * @date 2024-01-14
 * @updated 2025-02-11
 */
package com.yowyob.geo.controller;

import com.yowyob.geo.dto.DistanceRequest;
import com.yowyob.geo.dto.DistanceResponse;
import com.yowyob.geo.dto.GeoLocationDto;
import com.yowyob.geo.dto.GeocodeResponse;
import com.yowyob.geo.service.GeoService;
import com.yowyob.geo.service.IpGeolocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/geo")
@RequiredArgsConstructor
public class GeoController {

    private final GeoService geoService;
    private final IpGeolocationService ipGeolocationService;

    @GetMapping("/geocode")
    public Mono<ResponseEntity<GeocodeResponse>> geocode(@RequestParam String address) {
        return geoService.geocode(address)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping("/distance")
    public ResponseEntity<DistanceResponse> calculateDistance(@RequestBody DistanceRequest request) {
        DistanceResponse response = geoService.calculateDistance(
                request.getLat1(), request.getLon1(),
                request.getLat2(), request.getLon2());
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
        return ipGeolocationService.getLocationFromIp(finalIp)
                .map(ResponseEntity::ok)
                .doOnError(
                        error -> System.err
                                .println("Error getting IP location for " + finalIp + ": " + error.getMessage()));
    }

    @GetMapping("/route")
    public Mono<ResponseEntity<com.yowyob.geo.dto.RouteResponse>> getRoute(
            @RequestParam double startLat, @RequestParam double startLon,
            @RequestParam double endLat, @RequestParam double endLon,
            @RequestParam(required = false, defaultValue = "driving") String mode) {
        return geoService.getRoute(startLat, startLon, endLat, endLon, mode)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/health")
    public String health() {
        return "Geo Service is running!";
    }
}
