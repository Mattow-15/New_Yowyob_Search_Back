package com.yowyob.search.infrastructure.adapters.out.persistence;

import com.yowyob.search.application.ports.out.SearchHistoryRepositoryPort;
import com.yowyob.search.domain.model.SearchHistory;
import com.yowyob.search.infrastructure.adapters.out.persistence.repository.SearchHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class ElasticsearchHistoryAdapter implements SearchHistoryRepositoryPort {

    private final SearchHistoryRepository historyRepository;

    @Override
    public Mono<SearchHistory> save(SearchHistory history) {
        com.yowyob.search.infrastructure.adapters.out.persistence.document.SearchHistory doc = toDocument(history);
        return historyRepository.save(doc)
                .map(this::toDomain);
    }

    @Override
    public Flux<SearchHistory> findByUserId(String userId, int limit) {
        return historyRepository.findByUserIdOrderByTimestampDesc(userId, PageRequest.of(0, limit))
                .map(this::toDomain);
    }

    @Override
    public Mono<Void> deleteByUserId(String userId) {
        // Not implemented in original, returning empty Mono
        return Mono.empty();
    }

    private SearchHistory toDomain(com.yowyob.search.infrastructure.adapters.out.persistence.document.SearchHistory doc) {
        if (doc == null) return null;
        return SearchHistory.builder()
                .id(doc.getId())
                .userId(doc.getUserId())
                .query(doc.getQuery())
                .type(doc.getType())
                .city(doc.getCity())
                .timestamp(doc.getTimestamp())
                .build();
    }

    private com.yowyob.search.infrastructure.adapters.out.persistence.document.SearchHistory toDocument(SearchHistory domain) {
        if (domain == null) return null;
        return com.yowyob.search.infrastructure.adapters.out.persistence.document.SearchHistory.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .query(domain.getQuery())
                .type(domain.getType())
                .city(domain.getCity())
                .timestamp(domain.getTimestamp())
                .build();
    }
}
