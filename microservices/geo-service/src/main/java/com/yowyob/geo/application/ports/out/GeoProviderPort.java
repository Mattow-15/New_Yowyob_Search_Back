package com.yowyob.geo.application.ports.out;

import com.yowyob.geo.domain.model.GeocodeLocation;
import com.yowyob.geo.domain.model.Route;
import reactor.core.publisher.Mono;

public interface GeoProviderPort {
    Mono<GeocodeLocation> fetchFromNominatim(String address);
    Mono<Route> fetchRoute(double startLat, double startLon, double endLat, double endLon, String mode);
}
