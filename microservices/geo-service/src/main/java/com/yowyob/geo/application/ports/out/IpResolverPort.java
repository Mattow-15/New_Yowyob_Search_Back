package com.yowyob.geo.application.ports.out;

import com.yowyob.geo.domain.model.IpLocation;
import reactor.core.publisher.Mono;

public interface IpResolverPort {
    Mono<IpLocation> fetchFromIpApi(String ipAddress);
}
