package com.yowyob.search.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.yowyob.search.config.AiProperties;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@EnableConfigurationProperties(AiProperties.class)
public class LlmClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(LlmClient.class);
    private final AiProperties properties;
    private final WebClient webClient;

    public LlmClient(AiProperties properties, WebClient.Builder builder) {
        this.properties = properties;
        this.webClient = builder.build();
    }

    public Mono<String> generate(String prompt) {
        if (!properties.enabled()) {
            return Mono.empty();
        }
        return groq(prompt).switchIfEmpty(Mono.defer(() -> gemini(prompt)));
    }

    private Mono<String> groq(String prompt) {
        if (properties.groqApiKey().isBlank()) {
            return Mono.empty();
        }
        Map<String, Object> body = Map.of(
                "model", properties.groqModel(),
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "temperature", 0.2,
                "max_tokens", 1400);
        return webClient.post()
                .uri("https://api.groq.com/openai/v1/chat/completions")
                .header("Authorization", "Bearer " + properties.groqApiKey())
                .bodyValue(body)
                .retrieve()
                .bodyToMono(GroqResponse.class)
                .map(response -> response.choices() == null || response.choices().isEmpty()
                        ? "" : response.choices().getFirst().message().content())
                .filter(text -> text != null && !text.isBlank())
                .timeout(Duration.ofSeconds(15))
                .onErrorResume(error -> {
                    LOGGER.warn("Groq unavailable ({}); trying Gemini.", error.getClass().getSimpleName());
                    return Mono.empty();
                });
    }

    private Mono<String> gemini(String prompt) {
        if (properties.geminiApiKey().isBlank()) {
            return Mono.empty();
        }
        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of("temperature", 0.2, "maxOutputTokens", 1400));
        return webClient.post()
                .uri(builder -> builder
                        .scheme("https").host("generativelanguage.googleapis.com")
                        .path("/v1beta/models/{model}:generateContent")
                        .queryParam("key", properties.geminiApiKey())
                        .build(properties.geminiModel()))
                .bodyValue(body)
                .retrieve()
                .bodyToMono(GeminiResponse.class)
                .map(response -> response.candidates() == null || response.candidates().isEmpty()
                        ? "" : response.candidates().getFirst().content().parts().getFirst().text())
                .filter(text -> text != null && !text.isBlank())
                .timeout(Duration.ofSeconds(15))
                .onErrorResume(error -> {
                    LOGGER.warn("Gemini unavailable ({}).", error.getClass().getSimpleName());
                    return Mono.empty();
                });
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GroqResponse(List<Choice> choices) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Choice(Message message) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Message(String content) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    record GeminiResponse(List<Candidate> candidates) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Candidate(Content content) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Content(List<Part> parts) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Part(String text) {}
}
