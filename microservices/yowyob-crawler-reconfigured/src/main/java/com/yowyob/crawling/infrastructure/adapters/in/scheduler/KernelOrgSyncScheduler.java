package com.yowyob.crawling.infrastructure.adapters.in.scheduler;

import com.yowyob.crawling.application.services.KernelOrgSyncService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Declencheur (adaptateur pilote) du sync Kernel.
 * Cadence ESPACEE par defaut (toutes les 6h) -- surtout pas la minute : quotas
 * Kernel + donnee maitre lente. Configurable via kernel.ingestion.sync-cron.
 *
 * Garde une borne incrementale en memoire ; au 1er run (since=null) -> sync complet.
 * Pour survivre aux redemarrages, persiste 'lastSync' (BD/Redis) plus tard.
 */
@Component
public class KernelOrgSyncScheduler {

    private final KernelOrgSyncService syncService;
    private volatile Instant lastSync = null;

    public KernelOrgSyncScheduler(KernelOrgSyncService syncService) {
        this.syncService = syncService;
    }

    @Scheduled(cron = "${kernel.ingestion.sync-cron:0 0 */6 * * *}")
    public void run() {
        Instant startedAt = Instant.now();
        syncService.sync(lastSync);
        lastSync = startedAt; // prochaine fois : seulement les modifications depuis ce run
    }
}
