package com.yowyob.search.application.services;

import com.yowyob.search.application.ports.in.ManageSearchHistoryUseCase;
import com.yowyob.search.application.ports.in.SearchUseCase;
import com.yowyob.search.application.ports.out.EmbeddingClientPort;
import com.yowyob.search.application.ports.out.GeoServiceClientPort;
import com.yowyob.search.application.ports.out.ProductSearchRepositoryPort;
import com.yowyob.search.application.ports.out.SearchHistoryRepositoryPort;
import com.yowyob.search.domain.logic.KeywordParser;
import com.yowyob.search.domain.model.GeocodeLocation;
import com.yowyob.search.domain.model.IpLocation;
import com.yowyob.search.domain.model.Product;
import com.yowyob.search.domain.model.SearchHistory;
import com.yowyob.search.domain.model.SearchQueryResult;
import com.yowyob.search.domain.model.SearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class SearchApplicationService implements SearchUseCase, ManageSearchHistoryUseCase {

    private final ProductSearchRepositoryPort productRepository;
    private final SearchHistoryRepositoryPort historyRepository;
    private final GeoServiceClientPort geoServiceClient;
    private final EmbeddingClientPort embeddingClient;

    private final KeywordParser keywordParser = new KeywordParser();

    @Override
    public Mono<SearchQueryResult> search(String query, String type, String city, String userId, String ipAddress) {
        log.info("Searching: query={}, type={}, city={}, userId={}, ip={}", query, type, city, userId, ipAddress);

        KeywordParser.ParsedQueryResult parsed = keywordParser.parseWithCity(query);

        if (parsed.isProximitySearch && ipAddress != null) {
            log.info("Proximity search detected via IP: {}", ipAddress);
            return geoServiceClient.getLocationFromIp(ipAddress)
                    .flatMap(location -> {
                        if (location.getLatitude() != null && location.getLongitude() != null) {
                            return searchByUserProximity(query, location.getLatitude(), location.getLongitude(), type);
                        }
                        return Mono.empty();
                    })
                    .switchIfEmpty(Mono.defer(() -> executeHybridIntentSearch(parsed, query, type, city, userId)));
        }

        return executeHybridIntentSearch(parsed, query, type, city, userId);
    }

    private Mono<SearchQueryResult> executeHybridIntentSearch(
            KeywordParser.ParsedQueryResult parsed, String originalQuery,
            String type, String city, String userId) {

        Mono<SearchQueryResult> bm25Mono = executeStandardSearch(parsed, originalQuery, type, city, userId);

        Mono<float[]> vectorMono = embeddingClient.embed(originalQuery)
                .defaultIfEmpty(new float[0]);

        return Mono.zip(bm25Mono, vectorMono)
                .flatMap(tuple -> {
                    SearchQueryResult bm25Response = tuple.getT1();
                    float[] vector = tuple.getT2();

                    if (vector.length == 0) {
                        log.warn("Embedding indisponible — résultats BM25 uniquement pour: {}", originalQuery);
                        return Mono.just(bm25Response);
                    }

                    log.info("Recherche hybride BM25+KNN pour: {}", originalQuery);
                    return productRepository.searchKnn(vector, 20)
                            .map(p -> new SearchResult(p, null))
                            .collectList()
                            .map(knnResults -> {
                                SearchQueryResult knnResponse = SearchQueryResult.builder()
                                        .success(true)
                                        .query(originalQuery)
                                        .total(knnResults.size())
                                        .results(knnResults)
                                        .build();
                                return mergeResults(bm25Response, knnResponse, originalQuery);
                            });
                });
    }

    private SearchQueryResult mergeResults(SearchQueryResult bm25, SearchQueryResult knn, String query) {
        Map<String, SearchResult> seen = new LinkedHashMap<>();

        if (bm25.getResults() != null) {
            bm25.getResults().forEach(sr -> seen.put(sr.getProduct().getId(), sr));
        }
        if (knn.getResults() != null) {
            knn.getResults().forEach(sr -> seen.putIfAbsent(sr.getProduct().getId(), sr));
        }

        List<SearchResult> merged = prioritizeOfficial(new ArrayList<>(seen.values()));

        return SearchQueryResult.builder()
                .success(true)
                .query(query)
                .total(merged.size())
                .results(merged)
                .build();
    }

    private List<SearchResult> prioritizeOfficial(List<SearchResult> results) {
        if (results == null || results.size() < 2) return results;
        List<SearchResult> ordered = new ArrayList<>(results);
        ordered.sort((a, b) -> rankOfficial(a) - rankOfficial(b));
        return ordered;
    }

    private int rankOfficial(SearchResult sr) {
        return "KERNEL_ORG".equals(sr.getProduct().getSource()) ? 0 : 1;
    }

    private Mono<SearchQueryResult> executeStandardSearch(KeywordParser.ParsedQueryResult parsed,
            String originalQuery, String type, String city, String userId) {

        String effectiveCity = city;
        if (effectiveCity == null && parsed.extractedCity != null) {
            effectiveCity = parsed.extractedCity;
        }

        Flux<Product> resultFlux = (parsed.query != null && !parsed.query.isEmpty())
                ? productRepository.searchNative(parsed.query, parsed.inferredCategory, parsed.extractedCity)
                : productRepository.findAll();

        final String filterType = type;
        if (type != null && !"all".equalsIgnoreCase(type)) {
            resultFlux = resultFlux.filter(doc -> matchesType(doc, filterType));
        }

        resultFlux = resultFlux.distinct(doc -> {
            String t = doc.getTitle() != null ? doc.getTitle().toLowerCase().trim() : "";
            String c = doc.getCity() != null ? doc.getCity().toLowerCase().trim() : "";
            return t + "|" + c;
        });

        final String finalCity = effectiveCity;
        if (finalCity != null && !finalCity.isEmpty()) {
            final String normalizedFilterCity = normalizeAccents(finalCity).toLowerCase();
            resultFlux = resultFlux.filter(doc -> {
                if (doc.getCity() == null) return false;
                boolean directMatch = doc.getCity().equalsIgnoreCase(finalCity);
                boolean normalizedMatch = normalizeAccents(doc.getCity()).toLowerCase().contains(normalizedFilterCity);
                return directMatch || normalizedMatch;
            });
        }

        final String parsedQueryFinal = parsed.query;
        return resultFlux
                .map(p -> new SearchResult(p, null))
                .collectList()
                .map(results -> {
                    List<SearchResult> ordered = prioritizeOfficial(results);
                    return SearchQueryResult.builder()
                            .success(true)
                            .query(parsedQueryFinal)
                            .total(ordered.size())
                            .results(ordered)
                            .build();
                })
                .flatMap(response -> {
                    if (userId != null && parsedQueryFinal != null && !parsedQueryFinal.isEmpty()) {
                        return saveSearch(userId, parsedQueryFinal, type, city).thenReturn(response);
                    }
                    return Mono.just(response);
                });
    }

    private boolean matchesType(Product doc, String filterType) {
        String ft = filterType.toLowerCase();
        String docServiceType = doc.getServiceType() != null ? doc.getServiceType().toLowerCase() : "";
        String docType = doc.getType() != null ? doc.getType().toLowerCase() : "";

        if (ft.equals("shop")) {
            return "user".equals(docServiceType) || "shop".equals(docType);
        } else if (ft.equals("product") || ft.equals("products")) {
            return "listing".equals(docServiceType) || "product".equals(docType);
        } else if (ft.equals("service") || ft.equals("services")) {
            return "service".equals(docType);
        }
        return ft.equals(docServiceType) || ft.equals(docType);
    }

    @Override
    public Mono<SearchQueryResult> searchByUserProximity(String query, Double userLatitude, Double userLongitude, String type) {
        log.info("Searching by user proximity: query={}, lat={}, lon={}", query, userLatitude, userLongitude);

        if (userLatitude == null || userLongitude == null) {
            return search(query, type, null, null, null);
        }

        KeywordParser.ParsedQueryResult parsed = keywordParser.parseWithCity(query);
        Double proximityRadius = parsed.proximityRadius != null ? parsed.proximityRadius : 10.0;

        Flux<Product> resultFlux = productRepository.searchNative(parsed.query, null, null);

        return resultFlux
                .filter(doc -> {
                    if (doc.getLatitude() != null && doc.getLongitude() != null) {
                        double distance = haversine(userLatitude, userLongitude, doc.getLatitude(), doc.getLongitude());
                        return distance <= proximityRadius;
                    }
                    return false;
                })
                .map(doc -> {
                    double distance = haversine(userLatitude, userLongitude, doc.getLatitude(), doc.getLongitude());
                    return new SearchResult(doc, distance);
                })
                .collectList()
                .map(results -> {
                    results.sort((a, b) -> {
                        Double distA = a.getDistanceKm() != null ? a.getDistanceKm() : Double.MAX_VALUE;
                        Double distB = b.getDistanceKm() != null ? b.getDistanceKm() : Double.MAX_VALUE;
                        return Double.compare(distA, distB);
                    });
                    return SearchQueryResult.builder()
                            .success(true)
                            .query(query)
                            .total(results.size())
                            .results(results)
                            .build();
                });
    }

    @Override
    public Mono<SearchQueryResult> searchByProximity(String query, String city, Double radiusKm) {
        log.info("Searching by proximity: query={}, city={}, radius={}", query, city, radiusKm);

        return geoServiceClient.geocode(city)
                .flatMap(geoLocation -> {
                    double radius = radiusKm != null ? radiusKm : 10.0;
                    return productRepository.searchGeoDistance(geoLocation.getLatitude(), geoLocation.getLongitude(), radius, query)
                            .map(doc -> {
                                double distance = haversine(geoLocation.getLatitude(), geoLocation.getLongitude(),
                                        doc.getLatitude() != null ? doc.getLatitude() : 0,
                                        doc.getLongitude() != null ? doc.getLongitude() : 0);
                                return new SearchResult(doc, distance);
                            })
                            .collectList()
                            .map(results -> SearchQueryResult.builder()
                                    .success(true)
                                    .query(query)
                                    .total(results.size())
                                    .results(results)
                                    .build());
                })
                .switchIfEmpty(Mono.just(SearchQueryResult.builder().success(false).query(query).total(0).build()));
    }

    @Override
    public Mono<List<String>> autocomplete(String query) {
        if (query == null || query.trim().isEmpty()) {
            return Mono.just(List.of());
        }
        return productRepository.findByTitleOrDescription(query.trim())
                .map(Product::getTitle)
                .distinct()
                .take(5)
                .collectList();
    }

    @Override
    public Mono<Product> getProductById(String id) {
        log.info("Getting product details for ID: {}", id);
        return productRepository.findById(id);
    }

    @Override
    public Mono<Product> indexProduct(Product product) {
        if (product.getCity() != null && (product.getLatitude() == null || product.getLongitude() == null)) {
            return geoServiceClient.geocode(product.getCity())
                    .map(geo -> {
                        product.setLatitude(geo.getLatitude());
                        product.setLongitude(geo.getLongitude());
                        return product;
                    })
                    .switchIfEmpty(Mono.just(product))
                    .flatMap(productRepository::save);
        }
        return productRepository.save(product);
    }

    // ── ManageSearchHistoryUseCase ──────────────────────────────────

    @Override
    public Mono<Void> saveSearch(String userId, String query, String type, String city) {
        if (userId == null || userId.isEmpty()) return Mono.empty();
        SearchHistory history = SearchHistory.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .query(query)
                .type(type)
                .city(city)
                .timestamp(Instant.now())
                .build();
        return historyRepository.save(history)
                .doOnError(e -> log.error("Failed to save search history", e))
                .then();
    }

    @Override
    public Flux<SearchHistory> getUserHistory(String userId) {
        return historyRepository.findByUserId(userId, 50);
    }

    @Override
    public Mono<Void> clearHistory(String userId) {
        return historyRepository.deleteByUserId(userId);
    }

    // ── Haversine ──────────────────────────────────────────────────

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private String normalizeAccents(String input) {
        if (input == null) return "";
        return input.replace("é", "e").replace("È", "E").replace("É", "E").replace("è", "e").replace("ê", "e")
                .replace("ë", "e").replace("à", "a").replace("â", "a").replace("ù", "u").replace("û", "u")
                .replace("ô", "o").replace("ö", "o").replace("ç", "c").replace("Ç", "C").replace("î", "i")
                .replace("ï", "i");
    }
}
