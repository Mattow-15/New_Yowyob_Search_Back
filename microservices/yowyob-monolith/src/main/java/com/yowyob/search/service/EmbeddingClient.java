package com.yowyob.search.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class EmbeddingClient {

    private final WebClient webClient;

    public EmbeddingClient(
            WebClient.Builder webClientBuilder,
            @Value("${EMBEDDING_SERVICE_URL:http://localhost:8000}") String embeddingServiceUrl) {
        
        log.info("Initializing EmbeddingClient with URL: {}", embeddingServiceUrl);
        
        this.webClient = webClientBuilder
                .baseUrl(embeddingServiceUrl)
                .build();
    }

    /**
     * Appelle le microservice Python pour convertir un texte en vecteur (embedding).
     * @param text le texte à encoder
     * @return un tableau de float représentant le vecteur
     */
    public Mono<List<Float>> generateEmbedding(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Mono.empty();
        }

        return webClient.post()
                .uri("/embed")
                .bodyValue(Map.of("text", text))
                .retrieve()
                .bodyToMono(EmbedResponse.class)
                .map(EmbedResponse::getVector)
                .doOnError(error -> log.error("Failed to generate embedding for text: '{}'. Error: {}", text, error.getMessage()))
                .onErrorResume(e -> Mono.empty()); // Fallback silencieux en cas d'erreur
    }

    private static class EmbedResponse {
        private List<Float> vector;
        private Integer dimensions;

        public List<Float> getVector() {
            return vector;
        }

        public void setVector(List<Float> vector) {
            this.vector = vector;
        }

        public Integer getDimensions() {
            return dimensions;
        }

        public void setDimensions(Integer dimensions) {
            this.dimensions = dimensions;
        }
    }
}
