package com.yowyob.search.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * WebClient dedie au Kernel : base URL + identite machine (X-Client-Id /
 * X-Api-Key) posees en headers par defaut sur CHAQUE requete. Le contexte
 * utilisateur (bearer, tenant, organisation) est ajoute par requete dans
 * l'adaptateur.
 */
@Configuration
@EnableConfigurationProperties({KernelProperties.class, YowyobJwtProperties.class})
public class KernelWebClientConfig {

    @Bean
    public WebClient kernelWebClient(KernelProperties properties, WebClient.Builder builder) {
        return builder
                .baseUrl(properties.api().url())
                .defaultHeader("X-Client-Id", properties.api().clientId())
                .defaultHeader("X-Api-Key", properties.api().apiKey())
                .build();
    }
}
