package com.yowyob.geo.infrastructure.adapters.out.cache;

import com.yowyob.geo.application.ports.out.GeoCachePort;
import com.yowyob.geo.domain.model.GeocodeLocation;
import com.yowyob.geo.domain.model.IpLocation;
import com.yowyob.geo.infrastructure.adapters.in.web.dto.GeoLocationDto;
import com.yowyob.geo.infrastructure.adapters.in.web.dto.GeocodeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RedisGeoCacheAdapter implements GeoCachePort {

    private final ReactiveRedisTemplate<String, GeocodeResponse> geocodeRedisTemplate;

    @Qualifier("reactiveRedisTemplateForGeoLocation")
    private final ReactiveRedisTemplate<String, GeoLocationDto> ipLocationRedisTemplate;

    @Override
    public Mono<GeocodeLocation> getGeocode(String key) {
        return geocodeRedisTemplate.opsForValue().get(key)
                .map(response -> GeocodeLocation.builder()
                        .address(response.getAddress())
                        .latitude(response.getLatitude())
                        .longitude(response.getLongitude())
                        .build());
    }

    @Override
    public Mono<Boolean> putGeocode(String key, GeocodeLocation location) {
        GeocodeResponse response = GeocodeResponse.builder()
                .address(location.getAddress())
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .build();
        return geocodeRedisTemplate.opsForValue().set(key, response, Duration.ofDays(30));
    }

    @Override
    public Mono<IpLocation> getIpLocation(String key) {
        return ipLocationRedisTemplate.opsForValue().get(key)
                .map(dto -> IpLocation.builder()
                        .city(dto.getCity())
                        .country(dto.getCountry())
                        .latitude(dto.getLatitude())
                        .longitude(dto.getLongitude())
                        .build());
    }

    @Override
    public Mono<Boolean> putIpLocation(String key, IpLocation location) {
        GeoLocationDto dto = GeoLocationDto.builder()
                .city(location.getCity())
                .country(location.getCountry())
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .build();
        return ipLocationRedisTemplate.opsForValue().set(key, dto, Duration.ofDays(30));
    }
}
