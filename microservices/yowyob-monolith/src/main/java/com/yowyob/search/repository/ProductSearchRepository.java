package com.yowyob.search.repository;

import com.yowyob.search.document.ProductDocument;
import org.springframework.data.elasticsearch.repository.ReactiveElasticsearchRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

/**
 * Repository Elasticsearch réactif pour les documents produits.
 * Fournit la recherche full-text sur les champs title et description.
 *
 * @author YowYob Team
 * @since 1.0.0
 */
@Repository
public interface ProductSearchRepository extends ReactiveElasticsearchRepository<ProductDocument, String> {
    Flux<ProductDocument> findByTitleContainingOrDescriptionContaining(String title, String description);
}