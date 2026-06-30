package com.yowyob.search.infrastructure.adapters.out.persistence.repository;

import com.yowyob.search.infrastructure.adapters.out.persistence.document.SearchHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ReactiveElasticsearchRepository;
import reactor.core.publisher.Flux;

public interface SearchHistoryRepository extends ReactiveElasticsearchRepository<SearchHistory, String> {
    Flux<SearchHistory> findByUserIdOrderByTimestampDesc(String userId, Pageable pageable);
}
