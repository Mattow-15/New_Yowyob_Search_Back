package com.yowyob.search.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration de l'integration Kernel (prefixe "kernel" dans application.yml).
 * Liaison par constructeur (record) -- Spring Boot 3.x.
 */
@ConfigurationProperties(prefix = "kernel")
public record KernelProperties(Api api, String jwksUri) {

    public record Api(String url, String clientId, String apiKey) {}
}
