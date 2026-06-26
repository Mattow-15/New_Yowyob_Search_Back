package com.yowyob.search.api;

import com.yowyob.search.domain.SearchDoc;
import com.yowyob.search.service.SearchService;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Recherche générique. {@code GET /api/search?q=...} scopée au tenant ({@code X-Tenant-Id}),
 * filtrable par {@code collection}.
 */
@RestController
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/api/search")
    public Mono<ResponseEntity<SearchResponse>> search(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam("q") String query,
            @RequestParam(value = "collection", required = false) String collection,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return searchService.search(tenantId, query, collection, page, size)
                .map(SearchController::toHit)
                .collectList()
                .map(hits -> ResponseEntity.ok(new SearchResponse(hits.size(), hits)));
    }

    private static SearchHit toHit(SearchDoc doc) {
        return new SearchHit(doc.collection(), doc.externalId(), doc.title(), doc.source());
    }

    public record SearchHit(String collection, String id, String title, Map<String, Object> source) {
    }

    public record SearchResponse(int count, List<SearchHit> results) {
    }
}
