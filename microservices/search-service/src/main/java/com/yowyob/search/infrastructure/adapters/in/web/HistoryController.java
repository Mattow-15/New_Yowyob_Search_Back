package com.yowyob.search.infrastructure.adapters.in.web;

import com.yowyob.search.application.ports.in.ManageSearchHistoryUseCase;
import com.yowyob.search.domain.model.SearchHistory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/search/history")
@RequiredArgsConstructor
@Slf4j
public class HistoryController {

    private final ManageSearchHistoryUseCase historyUseCase;

    @GetMapping
    public Flux<SearchHistory> getHistory(@RequestHeader(value = "X-User-Id") String userId) {
        log.info("Fetching history for User ID: {}", userId);
        return historyUseCase.getUserHistory(userId)
                .doOnComplete(() -> log.info("History fetch completed for user: {}", userId))
                .doOnError(e -> log.error("Error fetching history for user: {}", userId, e));
    }
}
