package com.yowyob.search.service;

import com.yowyob.search.config.EmbeddingProperties;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import java.time.Duration;

/**
 * Client réactif du micro-service {@code yowyob-embeddings}. Transforme un texte en vecteur dense.
 *
 * <p>Tolérant aux pannes : si la recherche sémantique est désactivée, si le texte est vide, ou si
 * le service est injoignable, renvoie {@link Mono#empty()} — l'appelant retombe alors sur la
 * recherche lexicale. L'embeddings n'est donc jamais un point de panne dur.
 */
@Service
@EnableConfigurationProperties(EmbeddingProperties.class)
public class EmbeddingClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddingClient.class);

    private final EmbeddingProperties properties;
    private final WebClient webClient;

    public EmbeddingClient(EmbeddingProperties properties, WebClient.Builder builder) {
        this.properties = properties;
        this.webClient = builder.baseUrl(properties.url()).build();
    }

    public boolean isEnabled() {
        return properties.enabled();
    }

    /**
     * Génère le vecteur d'un texte. Renvoie {@link Mono#empty()} en cas de désactivation, de texte
     * vide ou d'erreur (dégradation silencieuse vers le lexical).
     */
    public Mono<float[]> embed(String text) {
        if (!properties.enabled() || text == null || text.isBlank()) {
            return Mono.empty();
        }
        return webClient.post()
                .uri("/embed")
                .bodyValue(Map.of("text", text))
                .retrieve()
                .bodyToMono(EmbedResponse.class)
                .timeout(Duration.ofSeconds(5))
                .retryWhen(Retry.backoff(1, Duration.ofMillis(200)))
                .mapNotNull(EmbedResponse::vector)
                .map(EmbeddingClient::toArray)
                .doOnError(error -> LOGGER.warn("Embedding indisponible, repli lexical: {}", error.toString()))
                .onErrorResume(error -> Mono.empty());
    }

    private static float[] toArray(List<Float> vector) {
        if (vector == null || vector.isEmpty()) {
            return null;
        }
        float[] array = new float[vector.size()];
        for (int i = 0; i < vector.size(); i++) {
            array[i] = vector.get(i);
        }
        return array;
    }

    /** Réponse du micro-service embeddings : {@code {"vector":[...],"dimensions":384}}. */
    public record EmbedResponse(List<Float> vector, Integer dimensions) {
    }
}
