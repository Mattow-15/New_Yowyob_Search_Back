package com.yowyob.search.crawler;

import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Déclenchement manuel du sync Kernel : {@code POST /api/sync/organizations}.
 * Sous {@code /api/**}, soumis à l'auth Kernel. Présent uniquement si {@code kernel.sync.enabled=true}.
 */
@RestController
@RequestMapping("/api/sync")
@ConditionalOnProperty(prefix = "kernel.sync", name = "enabled", havingValue = "true")
public class KernelSyncController {

    private final KernelSyncService kernelSyncService;

    public KernelSyncController(KernelSyncService kernelSyncService) {
        this.kernelSyncService = kernelSyncService;
    }

    @PostMapping("/organizations")
    public Mono<ResponseEntity<Map<String, Object>>> run() {
        kernelSyncService.sync().subscribe();
        return Mono.just(ResponseEntity.accepted()
                .body(Map.of("status", "started", "message", "Sync Kernel lancé en arrière-plan")));
    }
}
