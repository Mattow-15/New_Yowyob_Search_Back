package com.yowyob.geo.infrastructure.adapters.out.geoip;

import com.yowyob.geo.application.ports.out.IpResolverPort;
import com.yowyob.geo.domain.model.IpLocation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@Slf4j
public class IpApiIpResolverAdapter implements IpResolverPort {

    private final WebClient webClient;

    @Value("${app.ipapi.url:http://ip-api.com}")
    private String ipapiUrl;

    public IpApiIpResolverAdapter(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    @Override
    public Mono<IpLocation> fetchFromIpApi(String ipAddress) {
        log.info("Fetching geolocation for IP via Adapter: {}", ipAddress);

        return webClient.get()
                .uri(ipapiUrl + "/json/{ip}", ipAddress)
                .header("User-Agent", "YowYob-Search-Service")
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    String city = (String) response.get("city");
                    String country = (String) response.get("country");
                    Object latObj = response.get("lat");
                    Object lonObj = response.get("lon");

                    double latitude = latObj != null ? Double.parseDouble(latObj.toString()) : 0.0;
                    double longitude = lonObj != null ? Double.parseDouble(lonObj.toString()) : 0.0;

                    return IpLocation.builder()
                            .city(city != null ? city : "Douala")
                            .country(country != null ? country : "Cameroon")
                            .latitude(latitude)
                            .longitude(longitude)
                            .build();
                })
                .doOnSuccess(result -> log.info("Successfully resolved IP: {}", result.getCity()));
    }
}
