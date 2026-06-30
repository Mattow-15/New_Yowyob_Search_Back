package com.yowyob.search.application.ports.out;

import com.yowyob.search.domain.model.SearchHistory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface SearchHistoryRepositoryPort {
    Mono<SearchHistory> save(SearchHistory history);
    Flux<SearchHistory> findByUserId(String userId, int limit);
    Mono<Void> deleteByUserId(String userId);
}
