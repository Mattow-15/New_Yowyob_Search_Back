package com.yowyob.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration du WebClient réactif pour les appels HTTP sortants.
 * Fournit un bean WebClient unique utilisé par les services (Geo,
 * IpGeolocation).
 *
 * @author YowYob Team
 * @since 1.0.0
 */
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        return builder.build();
    }
}
