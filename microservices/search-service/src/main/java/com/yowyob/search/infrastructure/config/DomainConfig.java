package com.yowyob.search.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yowyob.search.application.ports.out.*;
import com.yowyob.search.application.services.AiSearchApplicationService;
import com.yowyob.search.application.services.SearchApplicationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainConfig {

    // Spring Boot 4 auto-configures the Jackson 3 ObjectMapper (tools.jackson.databind),
    // not the Jackson 2 one. The application code still uses the Jackson 2 API
    // (com.fasterxml.jackson.databind.ObjectMapper), so we expose it explicitly.
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public SearchApplicationService searchApplicationService(
            ProductSearchRepositoryPort productRepository,
            SearchHistoryRepositoryPort historyRepository,
            GeoServiceClientPort geoServiceClient,
            EmbeddingClientPort embeddingClient) {
        return new SearchApplicationService(productRepository, historyRepository, geoServiceClient, embeddingClient);
    }

    @Bean
    public AiSearchApplicationService aiSearchApplicationService(
            SearchApplicationService searchUseCase,
            LlmClientPort llmClient,
            ObjectMapper objectMapper) {
        return new AiSearchApplicationService(searchUseCase, llmClient, objectMapper);
    }
}
