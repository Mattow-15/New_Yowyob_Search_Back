package com.yowyob.search.application.ports.out;

import reactor.core.publisher.Mono;

public interface LlmClientPort {
    Mono<String> generate(String prompt);
    Mono<String> generate(String prompt, int maxOutputTokens, int timeoutSeconds);
}
