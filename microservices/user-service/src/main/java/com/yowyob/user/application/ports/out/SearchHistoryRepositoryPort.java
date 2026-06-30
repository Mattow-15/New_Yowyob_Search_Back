package com.yowyob.user.application.ports.out;

import com.yowyob.user.domain.model.SearchHistory;

import java.util.List;
import java.util.UUID;

public interface SearchHistoryRepositoryPort {
    SearchHistory save(SearchHistory searchHistory);
    List<SearchHistory> findByUserIdOrderBySearchedAtDesc(UUID userId);
    void deleteByUserId(UUID userId);
}
