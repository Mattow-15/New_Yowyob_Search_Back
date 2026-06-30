package com.yowyob.geo.application.ports.in;

import com.yowyob.geo.domain.model.GeocodeLocation;
import com.yowyob.geo.domain.model.IpLocation;
import reactor.core.publisher.Mono;

public interface GeolocalizeUseCase {
    Mono<GeocodeLocation> geocode(String address);
    Mono<IpLocation> getLocationFromIp(String ipAddress);
}
