package com.yowyob.search.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yowyob.search.domain.SearchDoc;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class AiSearchService {

    private final SearchService searchService;
    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    public AiSearchService(SearchService searchService, LlmClient llmClient, ObjectMapper objectMapper) {
        this.searchService = searchService;
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
    }

    public Mono<AiResult> answer(SearchQuery query) {
        long startedAt = System.currentTimeMillis();
        return searchService.search(query).take(10).collectList().flatMap(documents -> {
            List<Map<String, Object>> sources = documents.stream().map(this::toSource).toList();
            if (documents.isEmpty()) {
                return Mono.just(new AiResult("Aucun résultat trouvé pour cette recherche.", "GENERAL",
                        query.rawQuery(), sources, List.of(), System.currentTimeMillis() - startedAt, true));
            }
            String prompt = buildPrompt(query.rawQuery(), documents);
            return llmClient.generate(prompt)
                    .defaultIfEmpty(fallback(documents))
                    .map(answer -> new AiResult(answer, "GENERAL", query.rawQuery(), sources, List.of(),
                            System.currentTimeMillis() - startedAt, true));
        });
    }

    public Mono<FaqResult> faq(SearchQuery query) {
        return searchService.search(query).take(8).collectList().flatMap(documents ->
                llmClient.generate(buildFaqPrompt(query.rawQuery(), documents))
                        .map(this::parseFaq)
                        .filter(items -> !items.isEmpty())
                        .map(items -> new FaqResult(items, true))
                        .defaultIfEmpty(new FaqResult(fallbackFaq(query.rawQuery()), false)));
    }

    private String buildPrompt(String query, List<SearchDoc> documents) {
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < documents.size(); i++) {
            SearchDoc doc = documents.get(i);
            context.append(i + 1).append(". ").append(doc.title());
            append(context, "ville", doc.source().get("city"));
            append(context, "catégorie", doc.source().get("category"));
            append(context, "adresse", doc.source().get("address"));
            append(context, "note", doc.source().get("rating"));
            context.append('\n');
        }
        return """
                Tu es l'assistant de recherche locale YowYob au Cameroun.
                Demande: "%s"
                Résultats réels autorisés:
                %s
                Réponds en français, de façon concise et utile. Utilise uniquement ces résultats.
                N'invente jamais de commerce, adresse, prix ou téléphone. Cite les noms exactement.
                """.formatted(query, context);
    }

    private String buildFaqPrompt(String query, List<SearchDoc> documents) {
        return buildPrompt(query, documents) + """

                Retourne uniquement un tableau JSON de 3 objets au format
                [{"question":"...","answer":"..."}]. Réponses courtes et actionnables.
                """;
    }

    private List<FaqItem> parseFaq(String raw) {
        try {
            int start = raw.indexOf('[');
            int end = raw.lastIndexOf(']');
            if (start < 0 || end <= start) return List.of();
            JsonNode root = objectMapper.readTree(raw.substring(start, end + 1));
            List<FaqItem> items = new ArrayList<>();
            for (JsonNode node : root) {
                String question = node.path("question").asText("").trim();
                String answer = node.path("answer").asText("").trim();
                if (!question.isEmpty() && !answer.isEmpty()) items.add(new FaqItem(question, answer));
            }
            return items;
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private Map<String, Object> toSource(SearchDoc doc) {
        Map<String, Object> source = new LinkedHashMap<>(doc.source());
        source.put("id", doc.externalId());
        source.put("title", doc.title());
        source.put("collection", doc.collection());
        if (doc.location() != null) {
            source.put("latitude", doc.location().getLat());
            source.put("longitude", doc.location().getLon());
        }
        return source;
    }

    private String fallback(List<SearchDoc> documents) {
        return "Voici les résultats les plus pertinents : " + documents.stream()
                .limit(5).map(SearchDoc::title).reduce((a, b) -> a + ", " + b).orElse("") + ".";
    }

    private List<FaqItem> fallbackFaq(String query) {
        return List.of(
                new FaqItem("Comment choisir le bon résultat pour « " + query + " » ?",
                        "Compare la proximité, la note et les informations de chaque fiche."),
                new FaqItem("Comment contacter un établissement ?",
                        "Utilise le téléphone ou le site affiché sur sa fiche."),
                new FaqItem("Les résultats sont-ils à jour ?",
                        "Ils proviennent de l'index YowYob alimenté par ses sources officielles et ses crawlers."));
    }

    private void append(StringBuilder target, String label, Object value) {
        if (value != null && !value.toString().isBlank()) target.append(" — ").append(label).append(": ").append(value);
    }

    public record AiResult(String aiAnswer, String intent, String rewrittenQuery,
            List<Map<String, Object>> sources, List<String> subQueries,
            long processingTimeMs, boolean aiMode) {}
    public record FaqResult(List<FaqItem> questions, boolean generated) {}
    public record FaqItem(String question, String answer) {}
}
