package com.yowyob.search.controller;

import com.yowyob.search.document.ProductDocument;
import com.yowyob.search.repository.ProductSearchRepository;
import com.yowyob.search.service.EmbeddingClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/migration")
@RequiredArgsConstructor
@Slf4j
public class MigrationController {

    private final ProductSearchRepository searchRepository;
    private final EmbeddingClient embeddingClient;

    /**
     * Endpoint temporaire pour migrer les anciens produits en ajoutant leur vecteur sémantique.
     * À n'utiliser qu'une seule fois après le déploiement.
     */
    @PostMapping("/embeddings")
    public Mono<String> migrateEmbeddings() {
        log.info("Starting embeddings migration process for existing products...");

        return searchRepository.findAll()
                // Filtrer les produits qui n'ont pas encore de vecteur
                .filter(product -> product.getTextVector() == null || product.getTextVector().length == 0)
                .flatMap(product -> {
                    String textToEmbed = String.format("%s %s %s",
                            product.getTitle() != null ? product.getTitle() : "",
                            product.getCategory() != null ? product.getCategory() : "",
                            product.getDescription() != null ? product.getDescription() : ""
                    ).trim();

                    if (!textToEmbed.isEmpty()) {
                        return embeddingClient.generateEmbedding(textToEmbed)
                                .flatMap(vectorList -> {
                                    if (vectorList != null && !vectorList.isEmpty()) {
                                        float[] vectorArray = new float[vectorList.size()];
                                        for (int i = 0; i < vectorList.size(); i++) {
                                            vectorArray[i] = vectorList.get(i);
                                        }
                                        product.setTextVector(vectorArray);
                                        return searchRepository.save(product)
                                                .doOnSuccess(p -> log.info("Migrated product: {}", p.getId()));
                                    }
                                    return Mono.just(product);
                                })
                                // On ne bloque pas tout le flux si une seule erreur survient
                                .onErrorResume(e -> {
                                    log.error("Failed to migrate product {}: {}", product.getId(), e.getMessage());
                                    return Mono.just(product);
                                });
                    }
                    return Mono.just(product);
                }, 5) // Concurrency de 5 pour ne pas surcharger le microservice Python
                .count()
                .map(count -> "Migration process triggered successfully. Processed products count: " + count);
    }
}
