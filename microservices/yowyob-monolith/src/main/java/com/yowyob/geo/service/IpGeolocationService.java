package com.yowyob.geo.service;

import com.yowyob.geo.dto.GeoLocationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

/**
 * Service for IP Geolocation
 * Resolves user's location from their IP address
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class IpGeolocationService {

    private final WebClient webClient;
    @Qualifier("reactiveRedisTemplateForGeoLocation")
    private final ReactiveRedisTemplate<String, GeoLocationDto> redisTemplate;

    @Value("${app.ipapi.url:https://ipapi.co}")
    private String ipapiUrl;

    /**
     * Get geolocation from IP address
     * Uses ipapi.co which provides free geolocation service
     * 
     * @param ipAddress The IP address to geolocate
     * @return Mono containing GeoLocationDto with coordinates
     */
    public Mono<GeoLocationDto> getLocationFromIp(String ipAddress) {
        // Validate IP
        if (ipAddress == null || ipAddress.isEmpty() || ipAddress.equals("127.0.0.1")) {
            log.warn("Invalid or localhost IP address: {}", ipAddress);
            // Return default location (Douala, Cameroon)
            return Mono.just(GeoLocationDto.builder()
                    .city("Douala")
                    .country("Cameroon")
                    .latitude(4.0511)
                    .longitude(9.7679)
                    .build());
        }

        String cacheKey = "geo:ip:" + ipAddress;

        // Try to get from cache first
        return redisTemplate.opsForValue().get(cacheKey)
                .switchIfEmpty(fetchFromIpApi(ipAddress)
                        .flatMap(response -> {
                            if (response == null)
                                return Mono.empty();
                            return redisTemplate.opsForValue()
                                    .set(cacheKey, response, Duration.ofDays(30))
                                    .thenReturn(response);
                        }))
                .doOnError(error -> log.error("Error geolocalizing IP {}: {}", ipAddress, error.getMessage()));
    }

    /**
     * Fetch geolocation from ipapi.co API
     * Free service, no authentication required
     * 
     * @param ipAddress The IP address
     * @return Mono with GeoLocationDto
     */
    private Mono<GeoLocationDto> fetchFromIpApi(String ipAddress) {
        log.info("Fetching geolocation for IP: {}", ipAddress);

        return webClient.get()
                .uri(ipapiUrl + "/{ip}/json/", ipAddress)
                .header("User-Agent", "YowYob-Search-Service")
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    String city = (String) response.get("city");
                    String country = (String) response.get("country_name");
                    Object latObj = response.get("latitude");
                    Object lonObj = response.get("longitude");

                    Double latitude = latObj != null ? Double.parseDouble(latObj.toString()) : null;
                    Double longitude = lonObj != null ? Double.parseDouble(lonObj.toString()) : null;

                    // Return GeoLocationDto with available values
                    GeoLocationDto.GeoLocationDtoBuilder builder = GeoLocationDto.builder()
                            .city(city)
                            .country(country);

                    if (latitude != null)
                        builder.latitude(latitude);
                    if (longitude != null)
                        builder.longitude(longitude);

                    return builder.build();
                })
                .doOnSuccess(result -> log.info("Successfully geolocated IP {}: {}, {}", ipAddress, result.getCity(),
                        result.getCountry()))
                .onErrorResume(error -> {
                    log.error("Error calling ipapi.co for IP {}: {}", ipAddress, error.getMessage());
                    // Fallback to default location
                    return Mono.just(GeoLocationDto.builder()
                            .city("Douala")
                            .country("Cameroon")
                            .latitude(4.0511)
                            .longitude(9.7679)
                            .build());
                });
    }
}
