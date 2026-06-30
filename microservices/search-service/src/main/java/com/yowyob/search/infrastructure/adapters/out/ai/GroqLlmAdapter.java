package com.yowyob.search.infrastructure.adapters.out.ai;

import com.yowyob.search.application.ports.out.LlmClientPort;
import com.yowyob.search.infrastructure.adapters.out.client.GroqClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class GroqLlmAdapter implements LlmClientPort {

    private final GroqClient groqClient;

    @Override
    public Mono<String> generate(String prompt) {
        return groqClient.generate(prompt);
    }

    @Override
    public Mono<String> generate(String prompt, int maxOutputTokens, int timeoutSeconds) {
        return groqClient.generate(prompt, maxOutputTokens, timeoutSeconds);
    }
}
