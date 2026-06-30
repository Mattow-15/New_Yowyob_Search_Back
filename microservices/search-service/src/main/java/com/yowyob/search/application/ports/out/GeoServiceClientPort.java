package com.yowyob.search.application.ports.out;

import com.yowyob.search.domain.model.GeocodeLocation;
import com.yowyob.search.domain.model.IpLocation;
import reactor.core.publisher.Mono;

public interface GeoServiceClientPort {
    Mono<GeocodeLocation> geocode(String city);
    Mono<IpLocation> getLocationFromIp(String ipAddress);
}
