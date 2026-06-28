package com.yowyob.search.service;

import co.elastic.clients.elasticsearch._types.KnnSearch;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.yowyob.search.config.EmbeddingProperties;
import com.yowyob.search.domain.SearchDoc;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class SearchService {

    private static final int MAX_SIZE = 100;

    private final ReactiveElasticsearchOperations operations;
    private final EmbeddingClient embeddingClient;
    private final EmbeddingProperties embeddingProperties;

    public SearchService(ReactiveElasticsearchOperations operations, EmbeddingClient embeddingClient,
            EmbeddingProperties embeddingProperties) {
        this.operations = operations;
        this.embeddingClient = embeddingClient;
        this.embeddingProperties = embeddingProperties;
    }

    /**
     * Recherche hybride scopée au tenant : combine le lexical (edge-ngram, saisie partielle) et le
     * kNN sémantique (embedding de la requête). {@code collection} optionnel restreint à un type.
     *
     * <p>Si l'embeddings est désactivé ou injoignable, dégrade automatiquement vers le lexical seul.
     */
    public Flux<SearchDoc> search(String tenantId, String query, String collection, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 20 : Math.min(size, MAX_SIZE);

        return embeddingClient.embed(query)
                .<org.springframework.data.elasticsearch.core.query.Query>map(vector ->
                        hybridQuery(tenantId, query, collection, vector, safePage, safeSize))
                .defaultIfEmpty(lexicalQuery(tenantId, query, collection, safePage, safeSize))
                .flatMapMany(q -> operations.search(q, SearchDoc.class).map(SearchHit::getContent));
    }

    /** Requête hybride : bool lexical (scopé tenant/collection) + kNN sur {@code textVector}. */
    private org.springframework.data.elasticsearch.core.query.Query hybridQuery(String tenantId, String query,
            String collection, float[] vector, int page, int size) {
        List<Float> queryVector = toList(vector);
        return NativeQuery.builder()
                .withQuery(q -> q.bool(b -> {
                    b.filter(f -> f.term(t -> t.field("tenantId").value(tenantId)));
                    if (collection != null && !collection.isBlank()) {
                        b.filter(f -> f.term(t -> t.field("collection").value(collection)));
                    }
                    b.must(m -> m.bool(inner -> {
                        inner.should(s -> s.match(mm -> mm.field("title").query(query).boost(2.0f)));
                        inner.should(s -> s.match(mm -> mm.field("content").query(query)));
                        inner.minimumShouldMatch("1");
                        return inner;
                    }));
                    return b;
                }))
                .withKnnSearches(KnnSearch.of(knn -> knn
                        .field("textVector")
                        .queryVector(queryVector)
                        .k(embeddingProperties.k())
                        .numCandidates(embeddingProperties.numCandidates())
                        .boost(embeddingProperties.boost())
                        .filter(knnFilters(tenantId, collection))))
                .withPageable(PageRequest.of(page, size))
                .build();
    }

    /** Requête lexicale seule (repli) : edge-ngram sur title/content, scopée tenant/collection. */
    private org.springframework.data.elasticsearch.core.query.Query lexicalQuery(String tenantId, String query,
            String collection, int page, int size) {
        Criteria fullText = new Criteria("content").matches(query)
                .or(new Criteria("title").matches(query));
        Criteria criteria = new Criteria("tenantId").is(tenantId).subCriteria(fullText);
        if (collection != null && !collection.isBlank()) {
            criteria = criteria.and(new Criteria("collection").is(collection));
        }
        return new CriteriaQuery(criteria, PageRequest.of(page, size));
    }

    /** Filtres du kNN : même périmètre tenant/collection que le lexical (pas de fuite inter-tenant). */
    private static List<Query> knnFilters(String tenantId, String collection) {
        List<Query> filters = new ArrayList<>();
        filters.add(Query.of(q -> q.term(t -> t.field("tenantId").value(tenantId))));
        if (collection != null && !collection.isBlank()) {
            filters.add(Query.of(q -> q.term(t -> t.field("collection").value(collection))));
        }
        return filters;
    }

    private static List<Float> toList(float[] vector) {
        List<Float> list = new ArrayList<>(vector.length);
        for (float value : vector) {
            list.add(value);
        }
        return list;
    }
}
