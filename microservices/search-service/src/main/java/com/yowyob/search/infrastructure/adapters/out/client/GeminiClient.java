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
public class GeminiClient implements LlmClient {

    private final WebClient webClient;
    private final String apiKey;
    private final String model;

    public GeminiClient(
            @Value("${gemini.api.key:}") String apiKey,
            @Value("${gemini.model:gemini-2.0-flash}") String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.webClient = WebClient.builder()
            .baseUrl("https://generativelanguage.googleapis.com")
            .build();
    }

    /**
     * Génère une réponse via Gemini Flash (budget par défaut : 1024 tokens, 5 s).
     * Retourne Mono.empty() si la clé est absente ou en cas d'erreur.
     */
    public Mono<String> generate(String prompt) {
        return generate(prompt, 1024, 5);
    }

    /**
     * Variante avec budget réglable. Le Mode IA (fan-out) a besoin d'une synthèse
     * plus longue et d'un délai plus large que l'Aperçu IA mono-recherche.
     *
     * @param maxOutputTokens plafond de tokens générés
     * @param timeoutSeconds  délai d'attente avant abandon
     */
    public Mono<String> generate(String prompt, int maxOutputTokens, int timeoutSeconds) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Clé Gemini absente — mode IA désactivé");
            return Mono.empty();
        }

        Map<String, Object> body = Map.of(
            "contents", List.of(
                Map.of("parts", List.of(
                    Map.of("text", prompt)
                ))
            ),
            "generationConfig", Map.of(
                "temperature", 0.3,       // peu créatif = plus factuel
                "maxOutputTokens", maxOutputTokens,
                "topP", 0.8
            ),
            "safetySettings", List.of(
                Map.of(
                    "category", "HARM_CATEGORY_HARASSMENT",
                    "threshold", "BLOCK_NONE"
                )
            )
        );

        return webClient.post()
            .uri("/v1beta/models/" + model + ":generateContent?key=" + apiKey)
            .bodyValue(body)
            .retrieve()
            .bodyToMono(GeminiResponse.class)
            .map(response -> {
                if (response.getCandidates() != null
                        && !response.getCandidates().isEmpty()) {
                    var candidate = response.getCandidates().get(0);
                    if (candidate.getContent() != null
                            && candidate.getContent().getParts() != null
                            && !candidate.getContent().getParts().isEmpty()) {
                        return candidate.getContent().getParts().get(0).getText();
                    }
                }
                return "";
            })
            .filter(text -> !text.isBlank())
            .timeout(java.time.Duration.ofSeconds(timeoutSeconds))
            .doOnError(e -> log.error("Erreur Gemini API : {}", e.getMessage()))
            .onErrorResume(e -> Mono.empty());
    }

    // ── DTOs internes ─────────────────────────────────────────────

    @lombok.Data
    static class GeminiResponse {
        private List<Candidate> candidates;
    }

    @lombok.Data
    static class Candidate {
        private Content content;
    }

    @lombok.Data
    static class Content {
        private List<Part> parts;
    }

    @lombok.Data
    static class Part {
        private String text;
    }
}
