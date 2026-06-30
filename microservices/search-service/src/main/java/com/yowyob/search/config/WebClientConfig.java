package com.yowyob.search.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration for WebClient used to call external services
 */
@Configuration
public class WebClientConfig {
    
    @Bean
    public WebClient webClient() {
        return WebClient.builder()
            .build();
    }
}
