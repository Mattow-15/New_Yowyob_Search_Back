package com.yowyob.search.infrastructure.adapters.out.client;

import com.yowyob.search.application.ports.out.GeoServiceClientPort;
import com.yowyob.search.domain.model.GeocodeLocation;
import com.yowyob.search.domain.model.IpLocation;
import com.yowyob.search.infrastructure.adapters.in.web.dto.GeoLocationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class GeoServiceHttpAdapter implements GeoServiceClientPort {

    private final WebClient webClient;

    @Value("${geo.service.url:http://localhost:8085}")
    private String geoServiceUrl;

    @Override
    public Mono<GeocodeLocation> geocode(String city) {
        if (city == null || city.isEmpty()) {
            return Mono.empty();
        }

        log.info("Geocoding city via Adapter: {}", city);

        return webClient
                .get()
                .uri(geoServiceUrl + "/api/geo/geocode?address={address}", city)
                .retrieve()
                .bodyToMono(GeoLocationDto.class)
                .map(dto -> GeocodeLocation.builder()
                        .address(dto.getCity() != null ? dto.getCity() : city)
                        .latitude(dto.getLatitude())
                        .longitude(dto.getLongitude())
                        .build())
                .doOnSuccess(result -> log.info("Successfully geocoded city {}: lat={}, lng={}",
                        city, result.getLatitude(), result.getLongitude()))
                .doOnError(error -> log.error("Failed to geocode city {}: {}", city, error.getMessage()))
                .onErrorResume(error -> {
                    log.warn("Geocoding failed for city {}, continuing without coordinates", city);
                    return Mono.empty();
                });
    }

    @Override
    public Mono<IpLocation> getLocationFromIp(String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty()) {
            return Mono.empty();
        }

        log.info("Getting geolocation for IP via Adapter: {}", ipAddress);

        return webClient
                .get()
                .uri(geoServiceUrl + "/api/geo/ip-location?ip={ip}", ipAddress)
                .retrieve()
                .bodyToMono(GeoLocationDto.class)
                .map(dto -> IpLocation.builder()
                        .city(dto.getCity())
                        .country(dto.getCountry())
                        .latitude(dto.getLatitude())
                        .longitude(dto.getLongitude())
                        .build())
                .doOnSuccess(result -> log.info("Successfully geolocated IP {}: {}, {}",
                        ipAddress, result.getCity(), result.getCountry()))
                .doOnError(error -> log.error("Failed to geolocate IP {}: {}", ipAddress, error.getMessage()))
                .onErrorResume(error -> {
                    log.warn("IP geolocation failed for {}, continuing without coordinates", ipAddress);
                    return Mono.empty();
                });
    }
}
