package com.yowyob.search.infrastructure.adapters.out.ai;

import com.yowyob.search.application.ports.out.EmbeddingClientPort;
import com.yowyob.search.infrastructure.adapters.out.client.EmbeddingClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class EmbeddingClientAdapter implements EmbeddingClientPort {

    private final EmbeddingClient embeddingClient;

    @Override
    public Mono<float[]> embed(String text) {
        return embeddingClient.embed(text);
    }

    @Override
    public String buildTextToEmbed(String title, String description, String category, String city) {
        return embeddingClient.buildTextToEmbed(title, description, category, city);
    }
}
