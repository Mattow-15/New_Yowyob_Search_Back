package com.yowyob.geo.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration for WebClient used by IP Geolocation Service
 */
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .build();
    }

    // Spring Boot 4 n'auto-configure plus de bean WebClient.Builder ; on l'expose
    // explicitement pour les adaptateurs qui l'injectent (Nominatim, IP-API…).
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
