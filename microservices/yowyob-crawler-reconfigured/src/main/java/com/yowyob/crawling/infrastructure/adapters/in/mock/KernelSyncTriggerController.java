package com.yowyob.crawling.infrastructure.adapters.in.mock;

import com.yowyob.crawling.application.services.KernelOrgSyncService;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Declencheur manuel du sync. Evite d'attendre le cron 6h -- indispensable pour
 * valider l'integration reelle (profil "docker") sans patienter 6h.
 * POST /mock/kernel/sync -> lance un sync complet et renvoie le nombre publie.
 *
 * /!\ Endpoint NON authentifie : ok pour labo/demo, a verrouiller (ou retirer)
 * pour une vraie prod exposee.
 */
@RestController
@Profile({"mock", "docker"})
public class KernelSyncTriggerController {

    private final KernelOrgSyncService syncService;

    public KernelSyncTriggerController(KernelOrgSyncService syncService) {
        this.syncService = syncService;
    }

    @PostMapping("/mock/kernel/sync")
    public Map<String, Object> trigger() {
        return Map.of("published", syncService.sync(null));
    }
}
