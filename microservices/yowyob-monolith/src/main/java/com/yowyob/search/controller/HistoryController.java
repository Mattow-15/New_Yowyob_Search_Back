package com.yowyob.search.controller;

import com.yowyob.search.document.SearchHistory;
import com.yowyob.search.service.SearchHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * Contrôleur REST pour l'historique de recherche Elasticsearch.
 * Expose l'endpoint de consultation de l'historique par utilisateur.
 *
 * @author YowYob Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/search/history")
@RequiredArgsConstructor
@Slf4j
public class HistoryController {
    private final SearchHistoryService historyService;

    /**
     * Récupère l'historique de recherche d'un utilisateur.
     *
     * @param user_id identifiant de l'utilisateur (header X-User-Id)
     * @return flux des entrées d'historique de recherche
     */
    @GetMapping
    public Flux<SearchHistory> getHistory(@RequestHeader(value = "X-User-Id") String user_id) {
        log.info("Fetching history for User ID: {}", user_id);
        return historyService.getUserHistory(user_id)
                .doOnComplete(() -> log.info("History fetch completed for user: {}", user_id))
                .doOnError(e -> log.error("Error fetching history for user: {}", user_id, e));
    }
}
