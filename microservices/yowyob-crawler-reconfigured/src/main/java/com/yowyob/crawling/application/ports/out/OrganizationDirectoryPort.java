package com.yowyob.crawling.application.ports.out;

import com.yowyob.crawling.domain.model.KernelAgency;
import java.time.Instant;
import java.util.List;

/**
 * Port de sortie : lecture des agences depuis organization-core.
 *
 * Pas de "findNearby" : le rapport ne decrit AUCUN endpoint de recherche geo
 * cote Kernel. On LISTE les agences (pagine, incremental via 'since'), et c'est
 * Elasticsearch qui fera la proximite une fois les agences indexees.
 */
public interface OrganizationDirectoryPort {

    /**
     * @param organizationId  organisation dont on liste les agences (en-tete X-Organization-Id ;
     *                        le listing est scope par organisation cote Kernel)
     * @param since  ne renvoyer que les agences modifiees apres cette date (null = tout)
     * @param page   index de page (0-based)
     * @param size   taille de page
     * @return la page d'agences ; une liste plus courte que 'size' signale la derniere page
     */
    List<KernelAgency> listAgencies(String organizationId, Instant since, int page, int size);
}
