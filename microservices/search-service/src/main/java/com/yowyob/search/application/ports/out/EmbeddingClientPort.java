package com.yowyob.search.application.ports.out;

import reactor.core.publisher.Mono;

public interface EmbeddingClientPort {
    Mono<float[]> embed(String text);
    String buildTextToEmbed(String title, String description, String category, String city);
}
