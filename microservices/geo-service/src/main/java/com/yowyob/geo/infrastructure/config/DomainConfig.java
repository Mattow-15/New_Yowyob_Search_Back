package com.yowyob.geo.infrastructure.config;

import com.yowyob.geo.application.ports.out.GeoCachePort;
import com.yowyob.geo.application.ports.out.GeoProviderPort;
import com.yowyob.geo.application.ports.out.IpResolverPort;
import com.yowyob.geo.application.services.GeoApplicationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainConfig {

    @Bean
    public GeoApplicationService geoApplicationService(
            GeoProviderPort geoProviderPort,
            IpResolverPort ipResolverPort,
            GeoCachePort geoCachePort) {
        return new GeoApplicationService(geoProviderPort, ipResolverPort, geoCachePort);
    }
}
