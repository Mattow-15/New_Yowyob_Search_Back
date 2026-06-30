package com.yowyob.geo.application.ports.out;

import com.yowyob.geo.domain.model.GeocodeLocation;
import com.yowyob.geo.domain.model.IpLocation;
import reactor.core.publisher.Mono;

public interface GeoCachePort {
    Mono<GeocodeLocation> getGeocode(String key);
    Mono<Boolean> putGeocode(String key, GeocodeLocation location);
    Mono<IpLocation> getIpLocation(String key);
    Mono<Boolean> putIpLocation(String key, IpLocation location);
}
