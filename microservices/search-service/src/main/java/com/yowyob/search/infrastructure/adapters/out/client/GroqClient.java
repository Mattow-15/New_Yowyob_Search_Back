package com.yowyob.search.infrastructure.adapters.out.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Fournisseur LLM via Groq (API compatible OpenAI : /chat/completions, Bearer token).
 * Gratuit, sans carte bancaire — choisi quand le free tier Gemini est à 0 (limit:0).
 *
 * @Primary : c'est le LLM actif. Pour rebasculer sur Gemini (ex: facturation activée),
 * déplacer @Primary sur {@link GeminiClient}.
 *
 * Clé via ${groq.api.key} (env GROQ_API_KEY). Modèle via ${groq.model}
 * (env GROQ_MODEL) pour éviter le piège du modèle retiré, sans rebuild.
 */
@Component
@Primary
@Slf4j
public class GroqClient implements LlmClient {

    private final WebClient webClient;
    private final String apiKey;
    private final String model;

    public GroqClient(
            @Value("${groq.api.key:}") String apiKey,
            @Value("${groq.model:llama-3.3-70b-versatile}") String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.webClient = WebClient.builder()
            .baseUrl("https://api.groq.com/openai/v1")
            .build();
    }

    @Override
    public Mono<String> generate(String prompt) {
        return generate(prompt, 1024, 5);
    }

    @Override
    public Mono<String> generate(String prompt, int maxOutputTokens, int timeoutSeconds) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Clé Groq absente — synthèse IA désactivée");
            return Mono.empty();
        }

        Map<String, Object> body = Map.of(
            "model", model,
            "messages", List.of(Map.of("role", "user", "content", prompt)),
            "temperature", 0.3,        // peu créatif = plus factuel
            "max_tokens", maxOutputTokens
        );

        return webClient.post()
            .uri("/chat/completions")
            .header("Authorization", "Bearer " + apiKey)
            .bodyValue(body)
            .retrieve()
            .bodyToMono(GroqResponse.class)
            .map(response -> {
                if (response.getChoices() != null && !response.getChoices().isEmpty()) {
                    var message = response.getChoices().get(0).getMessage();
                    if (message != null && message.getContent() != null) {
                        return message.getContent();
                    }
                }
                return "";
            })
            .filter(text -> !text.isBlank())
            .timeout(java.time.Duration.ofSeconds(timeoutSeconds))
            .doOnError(e -> log.error("Erreur Groq API : {}", e.getMessage()))
            .onErrorResume(e -> Mono.empty());
    }

    // ── DTOs internes (format OpenAI) ─────────────────────────────
    @lombok.Data static class GroqResponse { private List<Choice> choices; }
    @lombok.Data static class Choice { private Message message; }
    @lombok.Data static class Message { private String content; }
}
