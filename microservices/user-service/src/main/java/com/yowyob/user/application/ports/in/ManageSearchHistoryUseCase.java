package com.yowyob.user.application.ports.in;

import com.yowyob.user.domain.model.SearchHistory;

import java.util.List;
import java.util.UUID;

public interface ManageSearchHistoryUseCase {
    void addSearchHistory(UUID userId, String query);
    List<SearchHistory> getSearchHistory(UUID userId);
    void clearSearchHistory(UUID userId);
}
