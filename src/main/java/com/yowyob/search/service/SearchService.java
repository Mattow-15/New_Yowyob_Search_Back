package com.yowyob.search.service;

import com.yowyob.search.domain.SearchDoc;
import org.springframework.data.domain.PageRequest;
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

    public SearchService(ReactiveElasticsearchOperations operations) {
        this.operations = operations;
    }

    /**
     * Recherche full-text scopée au tenant. {@code collection} optionnel restreint à un type.
     */
    public Flux<SearchDoc> search(String tenantId, String query, String collection, int page, int size) {
        Criteria fullText = new Criteria("content").matches(query)
                .or(new Criteria("title").matches(query));
        Criteria criteria = new Criteria("tenantId").is(tenantId).subCriteria(fullText);
        if (collection != null && !collection.isBlank()) {
            criteria = criteria.and(new Criteria("collection").is(collection));
        }
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 20 : Math.min(size, MAX_SIZE);
        CriteriaQuery criteriaQuery = new CriteriaQuery(criteria, PageRequest.of(safePage, safeSize));
        return operations.search(criteriaQuery, SearchDoc.class).map(SearchHit::getContent);
    }
}
