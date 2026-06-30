package com.yowyob.geo.application.ports.in;

import com.yowyob.geo.domain.model.Distance;
import com.yowyob.geo.domain.model.Route;
import reactor.core.publisher.Mono;

public interface CalculateRouteUseCase {
    Distance calculateDistance(double lat1, double lon1, double lat2, double lon2);
    Mono<Route> getRoute(double startLat, double startLon, double endLat, double endLon, String mode);
}
