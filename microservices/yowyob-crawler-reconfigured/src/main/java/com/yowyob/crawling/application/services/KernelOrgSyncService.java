package com.yowyob.crawling.application.services;

import com.yowyob.crawling.domain.model.KernelAgency;
import com.yowyob.crawling.application.ports.out.KernelOrgIndexPort;
import com.yowyob.crawling.application.ports.out.OrganizationDirectoryPort;

import java.time.Instant;
import java.util.List;
import java.util.logging.Logger;

/**
 * Cas d'utilisation Voie 1 : synchroniser les agences du Kernel vers l'index.
 * Parcourt organization-core page par page (incremental depuis 'since'), et
 * publie chaque agence sur le pipeline d'indexation. Java pur, zero framework.
 */
public class KernelOrgSyncService {

    private static final Logger log = Logger.getLogger(KernelOrgSyncService.class.getName());

    private final OrganizationDirectoryPort directory;
    private final KernelOrgIndexPort index;
    private final List<String> organizationIds;
    private final int pageSize;

    public KernelOrgSyncService(OrganizationDirectoryPort directory,
                                KernelOrgIndexPort index,
                                List<String> organizationIds,
                                int pageSize) {
        this.directory = directory;
        this.index = index;
        this.organizationIds = (organizationIds == null) ? List.of() : organizationIds;
        this.pageSize = pageSize <= 0 ? 100 : pageSize;
    }

    /**
     * Synchronise CHAQUE organisation configuree (le listing Kernel est scope par
     * organisation). Une liste a 1 element couvre le cas mono-organisation.
     *
     * @param since borne incrementale (null = resynchronisation complete)
     * @return nombre total d'agences publiees, toutes organisations confondues
     */
    public int sync(Instant since) {
        if (organizationIds.isEmpty()) {
            log.warning("Aucune organisation configuree (kernel.ingestion.organization-ids) : rien a synchroniser");
            return 0;
        }
        int published = 0;
        for (String organizationId : organizationIds) {
            published += syncOrganization(organizationId, since);
        }
        int total = published;
        log.info(() -> "Sync Kernel termine : " + total + " agences publiees sur "
                + organizationIds.size() + " organisation(s)");
        return published;
    }

    /** Pagine et publie les agences d'UNE organisation. */
    private int syncOrganization(String organizationId, Instant since) {
        int published = 0;
        int page = 0;
        while (true) {
            List<KernelAgency> batch = directory.listAgencies(organizationId, since, page, pageSize);
            if (batch == null || batch.isEmpty()) {
                break;
            }
            for (KernelAgency agency : batch) {
                try {
                    index.publish(agency);
                    published++;
                } catch (RuntimeException ex) {
                    log.warning("Publication agence " + agency.id() + " echouee: " + ex.getMessage());
                }
            }
            if (batch.size() < pageSize) {
                break; // derniere page
            }
            page++;
        }
        return published;
    }
}
