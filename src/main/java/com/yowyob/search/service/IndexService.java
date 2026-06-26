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

    public IndexService(SearchDocRepository repository) {
        this.repository = repository;
    }

    /** Upsert d'un document. */
    public Mono<SearchDoc> index(String tenantId, String collection, String externalId,
            Map<String, Object> source) {
        return repository.save(DocumentMapper.toDocument(tenantId, collection, externalId, source));
    }

    /** Upsert en lot. */
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
                .collectList()
                .flatMapMany(repository::saveAll);
    }

    /** Suppression d'un document. */
    public Mono<Void> delete(String tenantId, String collection, String externalId) {
        return repository.deleteById(SearchDoc.documentId(tenantId, collection, externalId));
    }
}
