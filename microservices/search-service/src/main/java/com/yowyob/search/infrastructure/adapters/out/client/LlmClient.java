package com.yowyob.search.infrastructure.adapters.out.client;

import reactor.core.publisher.Mono;

/**
 * Abstraction d'un fournisseur LLM (texte -> texte). Permet de basculer entre
 * providers (Gemini, Groq…) sans toucher aux orchestrateurs : on change juste
 * le bean @Primary. Tout retourne Mono.empty() en cas d'absence de clé ou d'erreur,
 * pour que l'appelant retombe sur son repli sans LLM.
 */
public interface LlmClient {

    /** Budget par défaut (1024 tokens, 5 s). */
    Mono<String> generate(String prompt);

    /** Budget réglable (la synthèse fan-out a besoin de plus de tokens et de temps). */
    Mono<String> generate(String prompt, int maxOutputTokens, int timeoutSeconds);
}
