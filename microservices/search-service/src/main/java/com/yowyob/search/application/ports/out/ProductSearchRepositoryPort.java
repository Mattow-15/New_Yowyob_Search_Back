package com.yowyob.search.application.ports.out;

import com.yowyob.search.domain.model.Product;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ProductSearchRepositoryPort {
    Mono<Product> findById(String id);
    Mono<Product> save(Product product);
    Flux<Product> findAll();
    Flux<Product> findByTitleOrDescription(String query);
    Flux<Product> searchNative(String query, String inferredCategory, String extractedCity);
    Flux<Product> searchKnn(float[] queryVector, int size);
    Flux<Product> searchGeoDistance(double latitude, double longitude, double radiusKm, String query);
}
