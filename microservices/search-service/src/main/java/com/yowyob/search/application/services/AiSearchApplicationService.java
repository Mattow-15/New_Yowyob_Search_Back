package com.yowyob.search.application.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yowyob.search.application.ports.in.AiSearchUseCase;
import com.yowyob.search.application.ports.in.SearchUseCase;
import com.yowyob.search.application.ports.out.LlmClientPort;
import com.yowyob.search.domain.logic.ContextBuilder;
import com.yowyob.search.domain.logic.IntentDetector;
import com.yowyob.search.domain.logic.QueryRewriter;
import com.yowyob.search.domain.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;

@Slf4j
@RequiredArgsConstructor
public class AiSearchApplicationService implements AiSearchUseCase {

    private final SearchUseCase searchUseCase;
    private final LlmClientPort llmClient;
    private final ObjectMapper objectMapper;

    private final IntentDetector intentDetector = new IntentDetector();
    private final QueryRewriter queryRewriter = new QueryRewriter();
    private final ContextBuilder contextBuilder = new ContextBuilder();

    private static final int MAX_SOURCES = 12;
    private static final int SYNTHESIS_MAX_TOKENS = 2048;
    private static final int SYNTHESIS_TIMEOUT_S = 12;
    private static final int FAQ_MAX_TOKENS = 1024;
    private static final int FAQ_TIMEOUT_S = 10;

    // ── Aperçu IA (RAG mono-recherche) ─────────────────────────────

    @Override
    public Mono<AiSearchQueryResult> answer(String query, String city) {
        long start = System.currentTimeMillis();

        IntentDetector.Intent intent = intentDetector.detect(query);
        log.info("Intent détecté : {} pour '{}'", intent, query);

        String rewrittenQuery = queryRewriter.rewrite(query, intent);
        log.info("Requête reformulée : '{}'", rewrittenQuery);

        return searchUseCase.search(rewrittenQuery, null, city, null, null)
                .flatMap(searchResult -> {
                    List<Product> docs = extractProducts(searchResult, 5);

                    String context = contextBuilder.build(docs, query);
                    String prompt = contextBuilder.buildPrompt(context, query, intent);

                    return llmClient.generate(prompt)
                            .defaultIfEmpty(buildFallbackAnswer(docs, intent))
                            .map(aiAnswer -> AiSearchQueryResult.builder()
                                    .aiAnswer(aiAnswer)
                                    .intent(intent.name())
                                    .rewrittenQuery(rewrittenQuery)
                                    .sources(docs)
                                    .processingTimeMs(System.currentTimeMillis() - start)
                                    .aiMode(true)
                                    .build());
                });
    }

    // ── Mode IA (query fan-out) ─────────────────────────────────────

    @Override
    public Mono<AiSearchQueryResult> answerAiMode(String query, String city) {
        long start = System.currentTimeMillis();

        if (query == null || query.isBlank()) {
            return Mono.just(emptyAiResult("Veuillez préciser votre demande.", start));
        }

        return llmClient.generate(buildDecomposePrompt(query))
                .map(this::parseSubQueries)
                .defaultIfEmpty(List.of())
                .flatMap(parsed -> {
                    List<SubQuery> subQueries = parsed.isEmpty()
                            ? List.of(new SubQuery(query, query, null))
                            : parsed;

                    List<String> labels = subQueries.stream().map(this::labelOf).toList();

                    List<Mono<SearchQueryResult>> searches = subQueries.stream()
                            .map(sq -> searchUseCase.search(sq.motsCles(), null, city, null, null)
                                    .onErrorResume(e -> {
                                        log.warn("Sous-recherche '{}' échouée : {}", sq.motsCles(), e.getMessage());
                                        return Mono.empty();
                                    }))
                            .toList();

                    return Flux.merge(searches).collectList().flatMap(responses -> {
                        List<Product> merged = dedupeById(responses);

                        if (merged.isEmpty()) {
                            return Mono.just(emptyAiResult("Je n'ai trouvé aucun commerce correspondant.", start));
                        }

                        List<Product> top = merged.size() > MAX_SOURCES ? merged.subList(0, MAX_SOURCES) : merged;

                        return llmClient.generate(buildSynthesisPrompt(query, top), SYNTHESIS_MAX_TOKENS, SYNTHESIS_TIMEOUT_S)
                                .defaultIfEmpty(buildFallbackAiAnswer(top))
                                .map(answer -> AiSearchQueryResult.builder()
                                        .aiAnswer(answer)
                                        .intent("AI_MODE")
                                        .rewrittenQuery(String.join(" | ", labels))
                                        .sources(top)
                                        .subQueries(labels)
                                        .processingTimeMs(System.currentTimeMillis() - start)
                                        .aiMode(true)
                                        .build());
                    });
                });
    }

    // ── FAQ dynamique ───────────────────────────────────────────────

    @Override
    public Mono<FaqQueryResult> generateFaq(String query, String city) {
        String q = (query == null) ? "" : query.trim();

        return searchUseCase.search(q, null, city, null, null)
                .onErrorResume(e -> Mono.just(SearchQueryResult.builder().results(List.of()).build()))
                .flatMap(sr -> {
                    List<Product> results = extractProducts(sr, 8);
                    return llmClient.generate(buildFaqPrompt(q, results), FAQ_MAX_TOKENS, FAQ_TIMEOUT_S)
                            .map(this::parseFaqItems)
                            .filter(list -> !list.isEmpty())
                            .map(list -> FaqQueryResult.builder().questions(list).generated(true).build())
                            .defaultIfEmpty(FaqQueryResult.builder().questions(fallbackFaq(q)).generated(false).build());
                });
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private List<Product> extractProducts(SearchQueryResult result, int limit) {
        if (result == null || result.getResults() == null) return List.of();
        return result.getResults().stream()
                .map(SearchResult::getProduct)
                .limit(limit)
                .toList();
    }

    private List<Product> dedupeById(List<SearchQueryResult> responses) {
        LinkedHashMap<String, Product> byId = new LinkedHashMap<>();
        for (SearchQueryResult resp : responses) {
            if (resp == null || resp.getResults() == null) continue;
            for (SearchResult sr : resp.getResults()) {
                if (sr.getProduct() != null && sr.getProduct().getId() != null) {
                    byId.putIfAbsent(sr.getProduct().getId(), sr.getProduct());
                }
            }
        }
        return new ArrayList<>(byId.values());
    }

    private String buildFallbackAnswer(List<Product> docs, IntentDetector.Intent intent) {
        if (docs.isEmpty()) return "Aucun résultat trouvé pour votre recherche.";
        StringBuilder sb = new StringBuilder();
        sb.append(intent == IntentDetector.Intent.RECOMMENDATION
                ? "Voici les établissements disponibles :\n\n"
                : "Voici ce que j'ai trouvé :\n\n");
        for (int i = 0; i < docs.size(); i++) {
            Product doc = docs.get(i);
            sb.append(i + 1).append(". **").append(safe(doc.getTitle())).append("**");
            if (doc.getRating() != null) sb.append(" — ⭐ ").append(doc.getRating()).append("/5");
            if (doc.getOpenNow() != null) sb.append(doc.getOpenNow() ? " — 🟢 Ouvert" : " — 🔴 Fermé");
            sb.append("\n");
            if (doc.getStreet() != null) {
                sb.append("   📍 ").append(doc.getStreet());
                if (doc.getCity() != null) sb.append(", ").append(doc.getCity());
                sb.append("\n");
            }
            if (doc.getPhone() != null) sb.append("   📞 ").append(doc.getPhone()).append("\n");
            sb.append("\n");
        }
        return sb.toString();
    }

    private String buildFallbackAiAnswer(List<Product> docs) {
        StringBuilder sb = new StringBuilder("Voici les commerces trouvés pour votre demande :\n\n");
        for (int i = 0; i < docs.size(); i++) {
            Product d = docs.get(i);
            sb.append(i + 1).append(". **").append(safe(d.getTitle())).append("**");
            if (d.getRating() != null) sb.append(" — ⭐ ").append(d.getRating()).append("/5");
            sb.append("\n");
        }
        return sb.toString();
    }

    private String buildDecomposePrompt(String query) {
        return """
                Tu es un assistant de recherche de commerces et services locaux au Cameroun.
                Demande de l'utilisateur : "%s".
                Décompose-la en 2 à 4 recherches de commerces distinctes et complémentaires.
                Réponds UNIQUEMENT par un tableau JSON valide, SANS texte autour, SANS balises Markdown,
                au format EXACT :
                [{"intitule":"...","motsCles":"...","categorie":"..."}]
                """.formatted(query);
    }

    private String buildSynthesisPrompt(String query, List<Product> docs) {
        StringBuilder list = new StringBuilder();
        for (int i = 0; i < docs.size(); i++) {
            Product d = docs.get(i);
            list.append(i + 1).append(". ").append(safe(d.getTitle()));
            if (d.getCategory() != null) list.append(" — ").append(d.getCategory());
            if (d.getRating() != null) list.append(" — note ").append(d.getRating()).append("/5");
            if (d.getSource() != null) list.append(" — source: ").append(d.getSource());
            list.append("\n");
        }
        return """
                Tu es l'assistant d'un moteur de recherche local au Cameroun.
                Demande de l'utilisateur : "%s".

                Commerces réellement trouvés dans notre base (la SEULE source autorisée) :
                %s
                Rédige une réponse utile et structurée qui répond à la demande, en t'appuyant sur ces commerces.
                RÈGLES STRICTES :
                - Utilise UNIQUEMENT les commerces de la liste ci-dessus. N'invente JAMAIS un commerce, une adresse ou un numéro.
                - Cite les commerces par leur nom EXACT.
                - Si la liste ne permet pas de répondre complètement, dis-le honnêtement.
                - Réponds en français, de façon concise et claire.
                """.formatted(query, list.toString());
    }

    private String buildFaqPrompt(String query, List<Product> docs) {
        StringBuilder ctx = new StringBuilder();
        for (Product d : docs) {
            ctx.append("- ").append(safe(d.getTitle()));
            if (d.getCategory() != null) ctx.append(" (").append(d.getCategory()).append(")");
            if (d.getRating() != null) ctx.append(", note ").append(d.getRating()).append("/5");
            if (d.getCity() != null) ctx.append(", ").append(d.getCity());
            if ("KERNEL_ORG".equals(d.getSource())) ctx.append(" [annuaire officiel]");
            ctx.append("\n");
        }
        String topic = query.isBlank() ? "la recherche de commerces et services locaux" : "« " + query + " »";
        return """
                Tu es l'assistant d'un moteur de recherche local au Cameroun (YowYob).
                Génère une FAQ « Autres questions posées » pour un utilisateur qui s'intéresse à %s.

                Commerces réellement trouvés dans notre base :
                %s
                Rédige 3 à 4 questions FRÉQUENTES et leurs réponses.
                RÈGLES :
                - Réponses COURTES (1-2 phrases), concrètes et ACTIONNABLES.
                - Réponds en français, en tutoyant.
                - Réponds UNIQUEMENT par un tableau JSON valide, SANS texte ni Markdown autour, au format EXACT :
                [{"question":"...","answer":"..."}]
                """.formatted(topic, ctx.length() == 0 ? "(aucun commerce trouvé)" : ctx.toString());
    }

    private List<FaqItem> parseFaqItems(String raw) {
        String json = extractJsonArray(raw);
        if (json == null) return List.of();
        try {
            List<FaqItem> items = objectMapper.readValue(json, new TypeReference<List<FaqItem>>() {});
            return items.stream()
                    .filter(it -> it != null && it.getQuestion() != null && !it.getQuestion().isBlank()
                            && it.getAnswer() != null && !it.getAnswer().isBlank())
                    .limit(4)
                    .toList();
        } catch (Exception e) {
            log.warn("FAQ JSON illisible : {}", e.getMessage());
            return List.of();
        }
    }

    private List<SubQuery> parseSubQueries(String raw) {
        String json = extractJsonArray(raw);
        if (json == null) return List.of();
        try {
            List<SubQuery> parsed = objectMapper.readValue(json, new TypeReference<List<SubQuery>>() {});
            return parsed.stream()
                    .filter(sq -> sq != null && sq.motsCles() != null && !sq.motsCles().isBlank())
                    .limit(4)
                    .toList();
        } catch (Exception e) {
            log.warn("Décomposition JSON illisible : {}", e.getMessage());
            return List.of();
        }
    }

    private String extractJsonArray(String raw) {
        if (raw == null) return null;
        int start = raw.indexOf('[');
        int end = raw.lastIndexOf(']');
        if (start < 0 || end < 0 || end <= start) return null;
        return raw.substring(start, end + 1);
    }

    private List<FaqItem> fallbackFaq(String query) {
        boolean hasQuery = !query.isBlank();
        if (hasQuery) {
            return List.of(
                faqItem("Comment choisir le bon résultat pour « " + query + " » ?",
                        "Trie par note et par proximité : les mieux notés et les plus proches remontent en premier."),
                faqItem("Comment contacter le commerce ou s'y rendre ?",
                        "Le téléphone et le site sont sur chaque fiche, et le bouton « Aller à » lance l'itinéraire."),
                faqItem("Que signifie le badge « Annuaire officiel » ?",
                        "Il marque les commerces vérifiés de l'annuaire officiel, affichés en priorité.")
            );
        }
        return List.of(
            faqItem("Comment trouver un commerce près de chez moi ?",
                    "Tape ce que tu cherches, puis trie par proximité. Le bouton « Aller à » lance l'itinéraire."),
            faqItem("Comment comparer les commerces ?",
                    "Chaque fiche affiche la note, les avis, les horaires et le téléphone."),
            faqItem("Que signifie le badge « Annuaire officiel » ?",
                    "Il marque les commerces vérifiés de l'annuaire officiel, mis en avant en haut de la liste.")
        );
    }

    private FaqItem faqItem(String q, String a) {
        return FaqItem.builder().question(q).answer(a).build();
    }

    private AiSearchQueryResult emptyAiResult(String message, long start) {
        return AiSearchQueryResult.builder()
                .aiAnswer(message)
                .intent("AI_MODE")
                .sources(List.of())
                .subQueries(List.of())
                .processingTimeMs(System.currentTimeMillis() - start)
                .aiMode(true)
                .build();
    }

    private String labelOf(SubQuery sq) {
        return (sq.intitule() != null && !sq.intitule().isBlank()) ? sq.intitule() : sq.motsCles();
    }

    private static String safe(String s) { return s == null ? "" : s; }

    public record SubQuery(String intitule, String motsCles, String categorie) {}
}
