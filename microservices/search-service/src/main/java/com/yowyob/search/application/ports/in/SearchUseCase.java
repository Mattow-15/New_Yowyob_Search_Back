package com.yowyob.search.application.ports.in;

import com.yowyob.search.domain.model.Product;
import com.yowyob.search.domain.model.SearchQueryResult;
import reactor.core.publisher.Mono;
import java.util.List;

public interface SearchUseCase {
    Mono<SearchQueryResult> search(String query, String type, String city, String userId, String ipAddress);
    Mono<SearchQueryResult> searchByUserProximity(String query, Double userLatitude, Double userLongitude, String type);
    Mono<SearchQueryResult> searchByProximity(String query, String city, Double radiusKm);
    Mono<List<String>> autocomplete(String query);
    Mono<Product> getProductById(String id);
    Mono<Product> indexProduct(Product product);
    /** Listing paginé sans mot-clé — énumération complète de l'index (sitemap frontend). */
    Mono<SearchQueryResult> listAllPaged(int page, int size);
}
