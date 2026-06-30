package com.yowyob.search.domain.logic;

import java.util.Set;

public class QueryRewriter {

    private static final Set<String> INTENT_NOISE = Set.of(
        "je", "veux", "besoin", "envie", "cherche", "chercher", "trouver",
        "j ai", "j'ai", "un", "une", "des", "du", "le", "la", "les", "un peu de"
    );

    public String rewrite(String query, IntentDetector.Intent intent) {
        if (query == null || query.isBlank()) return query;

        String rewritten = query.trim();

        rewritten = switch (intent) {
            case RECOMMENDATION -> {
                String cleaned = rewritten.toLowerCase();
                for (String noise : INTENT_NOISE) {
                    cleaned = cleaned.replace(noise, " ");
                }
                cleaned = cleaned.trim().replaceAll("\\s+", " ");
                yield cleaned.isEmpty() ? rewritten : cleaned;
            }
            case INFORMATION ->
                rewritten + " contact information";
            case NAVIGATION ->
                rewritten + " localisation adresse";
            default -> rewritten;
        };

        return rewritten.trim();
    }
}
