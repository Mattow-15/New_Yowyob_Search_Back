package com.yowyob.search.api;

import com.yowyob.search.domain.SearchDoc;
import com.yowyob.search.service.SearchQuery;
import com.yowyob.search.service.SearchService;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Recherche générique. {@code GET /api/search?q=...} scopée au tenant ({@code X-Tenant-Id}),
 * filtrable par {@code collection}. Combine lexical + sémantique. La proximité géo s'active soit
 * explicitement ({@code lat}/{@code lon}/{@code radiusKm} ou {@code city}), soit via le langage
 * naturel (« près de moi » → géoloc de l'IP appelante).
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
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "lat", required = false) Double lat,
            @RequestParam(value = "lon", required = false) Double lon,
            @RequestParam(value = "radiusKm", required = false) Double radiusKm,
            @RequestParam(value = "city", required = false) String city,
            ServerWebExchange exchange) {
        SearchQuery request = new SearchQuery(tenantId, query, collection, page, size, lat, lon, radiusKm, city,
                clientIp(exchange));
        return searchService.search(request)
                .map(SearchController::toHit)
                .collectList()
                .map(hits -> ResponseEntity.ok(new SearchResponse(hits.size(), hits)));
    }

    /** IP appelante réelle (derrière Traefik) pour la proximité « près de moi ». */
    private static String clientIp(ServerWebExchange exchange) {
        String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return exchange.getRequest().getRemoteAddress() != null
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : null;
    }

    private static SearchHit toHit(SearchDoc doc) {
        Double lat = doc.location() != null ? doc.location().getLat() : null;
        Double lon = doc.location() != null ? doc.location().getLon() : null;
        return new SearchHit(doc.collection(), doc.externalId(), doc.title(), lat, lon, doc.source());
    }

    public record SearchHit(String collection, String id, String title, Double latitude, Double longitude,
            Map<String, Object> source) {
    }

    public record SearchResponse(int count, List<SearchHit> results) {
    }
}
