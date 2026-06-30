package com.yowyob.search.repository;

import com.yowyob.search.document.SearchHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ReactiveElasticsearchRepository;
import reactor.core.publisher.Flux;

/**
 * Repository Elasticsearch réactif pour l'historique de recherche.
 *
 * @author YowYob Team
 * @since 1.0.0
 */
public interface ElasticSearchHistoryRepository extends ReactiveElasticsearchRepository<SearchHistory, String> {
    Flux<SearchHistory> findByUserIdOrderByTimestampDesc(String userId, Pageable pageable);
}
