package com.yowyob.geo.application.services;

import com.yowyob.geo.application.ports.in.CalculateRouteUseCase;
import com.yowyob.geo.application.ports.in.GeolocalizeUseCase;
import com.yowyob.geo.application.ports.out.GeoCachePort;
import com.yowyob.geo.application.ports.out.GeoProviderPort;
import com.yowyob.geo.application.ports.out.IpResolverPort;
import com.yowyob.geo.domain.model.Distance;
import com.yowyob.geo.domain.model.GeocodeLocation;
import com.yowyob.geo.domain.model.IpLocation;
import com.yowyob.geo.domain.model.Route;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class GeoApplicationService implements GeolocalizeUseCase, CalculateRouteUseCase {

    private final GeoProviderPort geoProviderPort;
    private final IpResolverPort ipResolverPort;
    private final GeoCachePort geoCachePort;

    private static final IpLocation DEFAULT_LOCATION = IpLocation.builder()
            .city("Douala")
            .country("Cameroon")
            .latitude(4.0511)
            .longitude(9.7679)
            .build();

    @Override
    public Mono<GeocodeLocation> geocode(String address) {
        String cacheKey = "geo:geocode:" + address.toLowerCase().trim().replaceAll("\\s+", "_");

        return geoCachePort.getGeocode(cacheKey)
                .switchIfEmpty(geoProviderPort.fetchFromNominatim(address)
                        .flatMap(location -> geoCachePort.putGeocode(cacheKey, location)
                                .thenReturn(location)));
    }

    @Override
    public Mono<IpLocation> getLocationFromIp(String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty() || ipAddress.equals("127.0.0.1")) {
            log.warn("Invalid or localhost IP address: {}. Returning default.", ipAddress);
            return Mono.just(DEFAULT_LOCATION);
        }

        String cacheKey = "geo:ip:" + ipAddress;

        return geoCachePort.getIpLocation(cacheKey)
                .switchIfEmpty(ipResolverPort.fetchFromIpApi(ipAddress)
                        .flatMap(location -> geoCachePort.putIpLocation(cacheKey, location)
                                .thenReturn(location)))
                .onErrorResume(error -> {
                    log.error("Error resolving IP location for {}: {}. Falling back to default.", ipAddress, error.getMessage());
                    return Mono.just(DEFAULT_LOCATION);
                });
    }

    @Override
    public Distance calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double distanceKm = haversine(lat1, lon1, lat2, lon2);
        return Distance.builder()
                .distanceKm(distanceKm)
                .distanceMiles(distanceKm * 0.621371)
                .build();
    }

    @Override
    public Mono<Route> getRoute(double startLat, double startLon, double endLat, double endLon, String mode) {
        return geoProviderPort.fetchRoute(startLat, startLon, endLat, endLon, mode)
                .onErrorResume(error -> {
                    log.error("Error fetching route from provider: {}. Falling back to straight line.", error.getMessage());
                    double straightDistance = haversine(startLat, startLon, endLat, endLon) * 1000;
                    String straightPolyline = String.format("[[%f,%f],[%f,%f]]", startLat, startLon, endLat, endLon);
                    return Mono.just(Route.builder()
                            .distance(straightDistance)
                            .duration(0.0)
                            .polyline(straightPolyline)
                            .build());
                });
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth radius in km

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }
}
