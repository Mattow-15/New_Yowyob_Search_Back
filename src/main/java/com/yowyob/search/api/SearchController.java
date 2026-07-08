package com.yowyob.search.api;

import com.yowyob.search.domain.SearchDoc;
import com.yowyob.search.repository.SearchDocRepository;
import com.yowyob.search.service.SearchQuery;
import com.yowyob.search.service.SearchService;
import com.yowyob.search.service.AiSearchService;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    private final AiSearchService aiSearchService;
    private final SearchDocRepository repository;

    public SearchController(SearchService searchService, AiSearchService aiSearchService,
            SearchDocRepository repository) {
        this.searchService = searchService;
        this.aiSearchService = aiSearchService;
        this.repository = repository;
    }

    @GetMapping("/api/search/ai")
    public Mono<ResponseEntity<AiSearchService.AiResult>> aiSearch(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam("q") String query,
            @RequestParam(value = "city", required = false) String city,
            ServerWebExchange exchange) {
        return aiSearchService.answer(new SearchQuery(tenantId, query, null, 0, 10,
                        null, null, null, city, clientIp(exchange)))
                .map(ResponseEntity::ok);
    }

    @GetMapping("/api/search/ai-mode")
    public Mono<ResponseEntity<AiSearchService.AiResult>> aiMode(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam("q") String query,
            @RequestParam(value = "city", required = false) String city,
            ServerWebExchange exchange) {
        return aiSearch(tenantId, query, city, exchange);
    }

    @GetMapping("/api/search/faq")
    public Mono<ResponseEntity<AiSearchService.FaqResult>> faq(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam("q") String query,
            @RequestParam(value = "city", required = false) String city,
            ServerWebExchange exchange) {
        return aiSearchService.faq(new SearchQuery(tenantId, query, null, 0, 8,
                        null, null, null, city, clientIp(exchange)))
                .map(ResponseEntity::ok);
    }

    @GetMapping("/api/search/{externalId}/details")
    public Mono<ResponseEntity<Object>> details(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable String externalId) {
        // L'ES doc ID = tenantId:collection:externalId — on tente les collections publiques connues
        return repository.findAllById(List.of(
                        SearchDoc.documentId(tenantId, "places", externalId),
                        SearchDoc.documentId(tenantId, "organization", externalId),
                        SearchDoc.documentId(tenantId, "products", externalId),
                        SearchDoc.documentId(tenantId, "services", externalId)))
                .next()
                .map(doc -> ResponseEntity.ok((Object) (doc.source() != null ? doc.source() : Map.of())))
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    /** Alias géo : le front envoie latitude/longitude/radius (km), on mappe vers /api/search. */
    @GetMapping("/api/search/near-me")
    public Mono<ResponseEntity<SearchResponse>> nearMe(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam(value = "q", defaultValue = "") String query,
            @RequestParam(value = "latitude") Double latitude,
            @RequestParam(value = "longitude") Double longitude,
            @RequestParam(value = "radius", defaultValue = "10") Double radius,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            ServerWebExchange exchange) {
        SearchQuery request = new SearchQuery(tenantId, query, null, page, size,
                latitude, longitude, radius, null, clientIp(exchange));
        return searchService.search(request)
                .map(SearchController::toHit)
                .collectList()
                .map(hits -> ResponseEntity.ok(new SearchResponse(hits.size(), hits)));
    }

    /** Autocomplétion : retourne les titres qui préfixent la saisie (edge-ngram). */
    @GetMapping("/api/search/autocomplete")
    public Mono<ResponseEntity<List<String>>> autocomplete(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam("q") String query,
            @RequestParam(value = "size", defaultValue = "8") int size) {
        if (query == null || query.isBlank()) {
            return Mono.just(ResponseEntity.ok(List.of()));
        }
        SearchQuery request = new SearchQuery(tenantId, query, null, 0, size,
                null, null, null, null, null);
        return searchService.search(request)
                .map(doc -> doc.title() != null ? doc.title() : "")
                .filter(t -> !t.isBlank())
                .distinct()
                .collectList()
                .map(ResponseEntity::ok);
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
