package com.yowyob.search.infrastructure.adapters.out.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class EmbeddingClient {

    private final WebClient webClient;

    public EmbeddingClient(
            @Value("${embedding.service.url:http://yowyob-embeddings:8000}")
            String embeddingUrl) {
        this.webClient = WebClient.builder()
            .baseUrl(embeddingUrl)
            .build();
    }

    /**
     * Génère un vecteur d'embedding pour un texte donné
     * Retourne un tableau de 384 floats
     */
    public Mono<float[]> embed(String text) {
        if (text == null || text.isBlank()) {
            return Mono.empty();
        }

        return webClient.post()
            .uri("/embed")
            .bodyValue(Map.of("text", text))
            .retrieve()
            .bodyToMono(EmbedResponse.class)
            .map(response -> {
                List<Float> vector = response.getVector();
                float[] result = new float[vector.size()];
                for (int i = 0; i < vector.size(); i++) {
                    result[i] = vector.get(i);
                }
                return result;
            })
            .doOnError(e -> log.warn(
                "Embedding service indisponible : {} — indexation sans vecteur",
                e.getMessage()))
            .onErrorReturn(new float[0]); // fallback silencieux
    }

    /**
     * Construit le texte à vectoriser depuis un document
     * Combine titre + description + catégorie pour plus de précision
     */
    public String buildTextToEmbed(String title, String description,
                                    String category, String city) {
        StringBuilder sb = new StringBuilder();
        if (title != null)       sb.append(title).append(" ");
        if (category != null)    sb.append(category).append(" ");
        if (city != null)        sb.append(city).append(" ");
        if (description != null) sb.append(description);
        return sb.toString().trim();
    }

    // DTO interne
    private static class EmbedResponse {
        private List<Float> vector;
        private int dimensions;

        public List<Float> getVector() { return vector; }
        public void setVector(List<Float> vector) { this.vector = vector; }
        public int getDimensions() { return dimensions; }
        public void setDimensions(int dimensions) { this.dimensions = dimensions; }
    }
}
