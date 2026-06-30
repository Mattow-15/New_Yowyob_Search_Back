package com.yowyob.search.application.ports.in;

import com.yowyob.search.domain.model.SearchHistory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ManageSearchHistoryUseCase {
    Mono<Void> saveSearch(String userId, String query, String type, String city);
    Flux<SearchHistory> getUserHistory(String userId);
    Mono<Void> clearHistory(String userId);
}
