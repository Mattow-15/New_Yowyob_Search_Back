package com.yowyob.search.crawler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Sync automatique planifié. Le cron vient de {@code kernel.sync.schedule-cron} ;
 * la valeur spéciale {@code "-"} (défaut) désactive la planification.
 */
@Component
@ConditionalOnProperty(prefix = "kernel.sync", name = "enabled", havingValue = "true")
public class KernelSyncScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(KernelSyncScheduler.class);

    private final KernelSyncService kernelSyncService;

    public KernelSyncScheduler(KernelSyncService kernelSyncService) {
        this.kernelSyncService = kernelSyncService;
    }

    @Scheduled(cron = "${kernel.sync.schedule-cron:-}")
    public void scheduledSync() {
        LOGGER.info("Sync Kernel planifié déclenché.");
        kernelSyncService.sync()
                .subscribe(
                        count -> LOGGER.info("Sync Kernel planifié terminé : {} org(s).", count),
                        error -> LOGGER.error("Sync Kernel planifié en échec : {}", error.getMessage()));
    }
}
