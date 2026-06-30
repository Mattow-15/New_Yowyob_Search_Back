package com.yowyob.crawling.infrastructure.adapters.in.scheduler;

import com.yowyob.crawling.application.services.BusinessBookSyncService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

/** Déclencheur du sync business book. 5 min par défaut (API interne incrémentale). */
@Component
public class BusinessBookSyncScheduler {

    private final BusinessBookSyncService syncService;
    private volatile Instant lastSync = null;

    public BusinessBookSyncScheduler(BusinessBookSyncService syncService) {
        this.syncService = syncService;
    }

    @Scheduled(cron = "${businessbook.sync-cron:0 */5 * * * *}")
    public void run() {
        Instant startedAt = Instant.now();
        syncService.sync(lastSync);
        lastSync = startedAt;
    }
}
