package com.yowyob.search.api;

import com.yowyob.search.domain.SearchDoc;
import com.yowyob.search.service.IndexService;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Ingestion générique. Tout projet pousse ses documents ici (HTTP), scopés par {@code X-Tenant-Id}.
 * {@code collection} = type logique libre ("products", "invoices", "clients"…).
 */
@RestController
@RequestMapping("/api/index")
public class IndexController {

    private final IndexService indexService;

    public IndexController(IndexService indexService) {
        this.indexService = indexService;
    }

    /** Upsert d'un document : PUT /api/index/{collection}/{id} */
    @PutMapping("/{collection}/{id}")
    public Mono<ResponseEntity<IndexedResponse>> upsert(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable String collection,
            @PathVariable String id,
            @RequestBody Map<String, Object> source) {
        return indexService.index(tenantId, collection, id, source)
                .map(doc -> ResponseEntity.ok(new IndexedResponse(doc.id(), 1)));
    }

    /** Upsert en lot : POST /api/index/{collection}/_bulk  (chaque élément doit porter un champ "id") */
    @PostMapping("/{collection}/_bulk")
    public Mono<ResponseEntity<IndexedResponse>> bulk(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable String collection,
            @RequestBody List<Map<String, Object>> documents) {
        return indexService.indexBulk(tenantId, collection, Flux.fromIterable(documents))
                .count()
                .map(count -> ResponseEntity.ok(new IndexedResponse(collection, count)));
    }

    /** Suppression : DELETE /api/index/{collection}/{id} */
    @DeleteMapping("/{collection}/{id}")
    public Mono<ResponseEntity<Void>> delete(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable String collection,
            @PathVariable String id) {
        return indexService.delete(tenantId, collection, id)
                .thenReturn(ResponseEntity.status(HttpStatus.NO_CONTENT).<Void>build());
    }

    public record IndexedResponse(String ref, long indexed) {
    }
}
