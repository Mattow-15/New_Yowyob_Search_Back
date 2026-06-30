package com.yowyob.search.infrastructure.adapters.in.web;

import com.yowyob.search.application.ports.in.AiSearchUseCase;
import com.yowyob.search.application.ports.in.SearchUseCase;
import com.yowyob.search.application.ports.out.GeoServiceClientPort;
import com.yowyob.search.domain.model.*;
import com.yowyob.search.infrastructure.adapters.in.web.dto.AiSearchResponse;
import com.yowyob.search.infrastructure.adapters.in.web.dto.FaqResponse;
import com.yowyob.search.infrastructure.adapters.in.web.dto.SearchResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Tag(name = "Search", description = "Endpoints for product search")
public class SearchController {

    private final SearchUseCase searchUseCase;
    private final AiSearchUseCase aiSearchUseCase;
    private final GeoServiceClientPort geoServiceClient;

    // ── Recherche classique ───────────────────────────────────────

    @GetMapping
    @Operation(summary = "Recherche classique")
    public Mono<SearchResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String city,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-Forwarded-For", required = false) String xForwardedFor,
            @RequestHeader(value = "Remote-Addr", required = false) String remoteAddr) {

        String ip = xForwardedFor != null ? xForwardedFor.split(",")[0].trim() : remoteAddr;
        if (ip == null) ip = "127.0.0.1";

        return searchUseCase.search(q, type, city, userId, ip)
                .map(this::toSearchResponse);
    }

    // ── Mode IA (RAG) ─────────────────────────────────────────────

    @GetMapping("/ai")
    @Operation(summary = "Recherche IA avec réponse générée (RAG)")
    public Mono<AiSearchResponse> aiSearch(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String city) {
        return aiSearchUseCase.answer(q, city)
                .map(this::toAiSearchResponse);
    }

    // ── Mode IA (query fan-out) ───────────────────────────────────

    @GetMapping("/ai-mode")
    @Operation(summary = "Mode IA (fan-out) : décompose une demande complexe, cherche en parallèle, synthétise")
    public Mono<AiSearchResponse> aiMode(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String city) {
        return aiSearchUseCase.answerAiMode(q, city)
                .map(this::toAiSearchResponse);
    }

    // ── FAQ dynamique ─────────────────────────────────────────────

    @GetMapping("/faq")
    @Operation(summary = "FAQ dynamique : génère des questions/réponses ancrées sur les résultats")
    public Mono<FaqResponse> faq(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String city) {
        return aiSearchUseCase.generateFaq(q, city)
                .map(this::toFaqResponse);
    }

    // ── Autres endpoints ──────────────────────────────────────────

    @GetMapping("/autocomplete")
    public Mono<List<String>> autocomplete(@RequestParam String q) {
        return searchUseCase.autocomplete(q);
    }

    @GetMapping("/proximity")
    public Mono<SearchResponse> searchByProximity(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String city,
            @RequestParam(required = false, defaultValue = "10") Double radius) {
        return searchUseCase.searchByProximity(q, city, radius)
                .map(this::toSearchResponse);
    }

    @GetMapping("/near-me")
    public Mono<SearchResponse> searchNearMe(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) String ip,
            @RequestParam(required = false) String type) {

        if (ip != null && !ip.isEmpty() && (latitude == null || longitude == null)) {
            return geoServiceClient.getLocationFromIp(ip)
                    .flatMap(location -> searchUseCase.searchByUserProximity(
                            q, location.getLatitude(), location.getLongitude(), type))
                    .switchIfEmpty(searchUseCase.searchByUserProximity(q, latitude, longitude, type))
                    .map(this::toSearchResponse);
        }
        return searchUseCase.searchByUserProximity(q, latitude, longitude, type)
                .map(this::toSearchResponse);
    }

    @GetMapping("/{id}/details")
    public Mono<com.yowyob.search.infrastructure.adapters.out.persistence.document.ProductDocument> getProductDetails(@PathVariable String id) {
        // Return the ProductDocument for now to maintain API compatibility
        return searchUseCase.getProductById(id)
                .map(p -> {
                    com.yowyob.search.infrastructure.adapters.out.persistence.document.ProductDocument doc = new com.yowyob.search.infrastructure.adapters.out.persistence.document.ProductDocument();
                    doc.setId(p.getId());
                    doc.setTitle(p.getTitle());
                    doc.setDescription(p.getDescription());
                    doc.setPrice(p.getPrice());
                    doc.setCategory(p.getCategory());
                    doc.setCity(p.getCity());
                    doc.setRating(p.getRating());
                    doc.setImages(p.getImages());
                    doc.setImageUrl(p.getImageUrl());
                    doc.setPhone(p.getPhone());
                    doc.setWebsite(p.getWebsite());
                    doc.setLatitude(p.getLatitude());
                    doc.setLongitude(p.getLongitude());
                    doc.setSource(p.getSource());
                    return doc;
                });
    }

    @PostMapping("/index")
    public Mono<com.yowyob.search.infrastructure.adapters.out.persistence.document.ProductDocument> indexProduct(
            @RequestBody com.yowyob.search.infrastructure.adapters.out.persistence.document.ProductDocument productDoc) {
        Product product = Product.builder()
                .id(productDoc.getId())
                .title(productDoc.getTitle())
                .description(productDoc.getDescription())
                .price(productDoc.getPrice())
                .serviceType(productDoc.getServiceType())
                .type(productDoc.getType())
                .category(productDoc.getCategory())
                .city(productDoc.getCity())
                .quartier(productDoc.getQuartier())
                .rating(productDoc.getRating())
                .latitude(productDoc.getLatitude())
                .longitude(productDoc.getLongitude())
                .images(productDoc.getImages())
                .imageUrl(productDoc.getImageUrl())
                .phone(productDoc.getPhone())
                .website(productDoc.getWebsite())
                .openingHours(productDoc.getOpeningHours())
                .reviewsCount(productDoc.getReviewsCount())
                .openNow(productDoc.getOpenNow())
                .priceLevel(productDoc.getPriceLevel())
                .reviewsSummary(productDoc.getReviewsSummary())
                .googleMapsUrl(productDoc.getGoogleMapsUrl())
                .street(productDoc.getStreet())
                .source(productDoc.getSource())
                .embedding(productDoc.getEmbedding())
                .build();
        return searchUseCase.indexProduct(product)
                .map(p -> productDoc); // Return the original doc for API compatibility
    }

    @GetMapping("/health")
    public Mono<String> health() {
        return Mono.just("Search Service OK — Architecture Hexagonale activée");
    }

    // ── Mapping domain → DTO ──────────────────────────────────────

    private SearchResponse toSearchResponse(SearchQueryResult result) {
        List<SearchResponse.ProductDto> dtos = result.getResults() == null ? List.of() :
                result.getResults().stream()
                        .map(sr -> {
                            Product p = sr.getProduct();
                            String effectiveType = p.getType();
                            if (effectiveType == null || effectiveType.isEmpty()) {
                                String st = p.getServiceType();
                                if ("listing".equalsIgnoreCase(st) || "businessbook".equalsIgnoreCase(st)) effectiveType = "product";
                                else if ("user".equalsIgnoreCase(st)) effectiveType = "shop";
                                else effectiveType = "product";
                            }
                            return SearchResponse.ProductDto.builder()
                                    .id(p.getId())
                                    .title(p.getTitle())
                                    .description(p.getDescription())
                                    .price(p.getPrice())
                                    .serviceType(p.getServiceType())
                                    .type(effectiveType)
                                    .category(p.getCategory())
                                    .city(p.getCity())
                                    .quartier(p.getQuartier())
                                    .rating(p.getRating())
                                    .images(p.getImages())
                                    .imageUrl(p.getImageUrl())
                                    .phone(p.getPhone())
                                    .website(p.getWebsite())
                                    .latitude(p.getLatitude())
                                    .longitude(p.getLongitude())
                                    .street(p.getStreet())
                                    .reviewsCount(p.getReviewsCount())
                                    .openNow(p.getOpenNow())
                                    .openingHours(p.getOpeningHours())
                                    .priceLevel(p.getPriceLevel())
                                    .reviewsSummary(p.getReviewsSummary())
                                    .googleMapsUrl(p.getGoogleMapsUrl())
                                    .source(p.getSource())
                                    .distanceKm(sr.getDistanceKm())
                                    .build();
                        })
                        .toList();

        return SearchResponse.builder()
                .success(result.isSuccess())
                .query(result.getQuery())
                .total(result.getTotal())
                .results(dtos)
                .build();
    }

    private AiSearchResponse toAiSearchResponse(AiSearchQueryResult result) {
        List<com.yowyob.search.infrastructure.adapters.out.persistence.document.ProductDocument> sources = result.getSources() == null ? List.of() :
                result.getSources().stream()
                        .map(p -> {
                            com.yowyob.search.infrastructure.adapters.out.persistence.document.ProductDocument doc = new com.yowyob.search.infrastructure.adapters.out.persistence.document.ProductDocument();
                            doc.setId(p.getId());
                            doc.setTitle(p.getTitle());
                            doc.setDescription(p.getDescription());
                            doc.setCategory(p.getCategory());
                            doc.setCity(p.getCity());
                            doc.setStreet(p.getStreet());
                            doc.setPhone(p.getPhone());
                            doc.setWebsite(p.getWebsite());
                            doc.setRating(p.getRating());
                            doc.setReviewsCount(p.getReviewsCount());
                            doc.setOpenNow(p.getOpenNow());
                            doc.setOpeningHours(p.getOpeningHours());
                            doc.setPriceLevel(p.getPriceLevel());
                            doc.setReviewsSummary(p.getReviewsSummary());
                            doc.setGoogleMapsUrl(p.getGoogleMapsUrl());
                            doc.setImageUrl(p.getImageUrl());
                            doc.setLatitude(p.getLatitude());
                            doc.setLongitude(p.getLongitude());
                            doc.setSource(p.getSource());
                            return doc;
                        })
                        .toList();

        return AiSearchResponse.builder()
                .aiAnswer(result.getAiAnswer())
                .intent(result.getIntent())
                .rewrittenQuery(result.getRewrittenQuery())
                .sources(sources)
                .subQueries(result.getSubQueries())
                .processingTimeMs(result.getProcessingTimeMs())
                .aiMode(result.isAiMode())
                .build();
    }

    private FaqResponse toFaqResponse(FaqQueryResult result) {
        List<FaqResponse.FaqItem> items = result.getQuestions() == null ? List.of() :
                result.getQuestions().stream()
                        .map(fi -> FaqResponse.FaqItem.builder()
                                .question(fi.getQuestion())
                                .answer(fi.getAnswer())
                                .build())
                        .toList();

        return FaqResponse.builder()
                .questions(items)
                .generated(result.isGenerated())
                .build();
    }
}
