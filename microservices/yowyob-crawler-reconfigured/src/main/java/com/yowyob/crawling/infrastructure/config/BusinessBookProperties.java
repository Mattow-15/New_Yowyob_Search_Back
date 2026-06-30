package com.yowyob.crawling.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "businessbook")
public record BusinessBookProperties(
        Api api,
        String serviceName,   // segment d'URL : /api/{serviceName}/search/documents
        String serviceToken,  // JWT s2s (placeholder ; voir ServiceTokenProvider)
        int pageSize
) {
    public record Api(String baseUrl) {}

    public BusinessBookProperties {
        if (pageSize <= 0) pageSize = 100;
    }
}
