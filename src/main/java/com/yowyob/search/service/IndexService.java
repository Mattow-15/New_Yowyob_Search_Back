package com.yowyob.search.service;

import com.yowyob.search.domain.SearchDoc;
import com.yowyob.search.repository.SearchDocRepository;
import java.util.Map;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class IndexService {

    private final SearchDocRepository repository;
    private final EmbeddingClient embeddingClient;

    public IndexService(SearchDocRepository repository, EmbeddingClient embeddingClient) {
        this.repository = repository;
        this.embeddingClient = embeddingClient;
    }

    /** Upsert d'un document (enrichi de son vecteur sémantique si l'embeddings est actif). */
    public Mono<SearchDoc> index(String tenantId, String collection, String externalId,
            Map<String, Object> source) {
        return withVector(DocumentMapper.toDocument(tenantId, collection, externalId, source))
                .flatMap(repository::save);
    }

    /** Upsert en lot (vectorisation puis sauvegarde groupée). */
    public Flux<SearchDoc> indexBulk(String tenantId, String collection,
            Flux<Map<String, Object>> documents) {
        return documents
                .map(doc -> {
                    Object id = doc.get("id");
                    if (id == null) {
                        throw new IllegalArgumentException("Each bulk item requires an 'id' field.");
                    }
                    return DocumentMapper.toDocument(tenantId, collection, id.toString(), doc);
                })
                .flatMapSequential(this::withVector)
                .collectList()
                .flatMapMany(repository::saveAll);
    }

    /** Suppression d'un document. */
    public Mono<Void> delete(String tenantId, String collection, String externalId) {
        return repository.deleteById(SearchDoc.documentId(tenantId, collection, externalId));
    }

    /**
     * Génère et attache le vecteur sémantique du document. En cas d'embeddings désactivé ou
     * injoignable, renvoie le document inchangé (recherche lexicale seule).
     */
    private Mono<SearchDoc> withVector(SearchDoc doc) {
        String text = DocumentMapper.embeddingText(doc);
        if (!embeddingClient.isEnabled() || text == null) {
            return Mono.just(doc);
        }
        return embeddingClient.embed(text)
                .map(doc::withTextVector)
                .defaultIfEmpty(doc);
    }
}
