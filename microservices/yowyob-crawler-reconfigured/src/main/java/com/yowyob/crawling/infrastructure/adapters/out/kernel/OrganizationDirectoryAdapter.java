package com.yowyob.crawling.infrastructure.adapters.out.kernel;

import com.yowyob.crawling.infrastructure.adapters.out.kernel.dto.KernelAgencyListResponse;
import com.yowyob.crawling.infrastructure.adapters.out.kernel.dto.KernelAgencyResponse;
import com.yowyob.crawling.infrastructure.config.KernelIngestionProperties;
import com.yowyob.crawling.domain.model.KernelAgency;
import com.yowyob.crawling.application.ports.out.OrganizationDirectoryPort;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Adaptateur sortant vers organization-core (kernel-core.yowyob.com), en IDENTITÉ MACHINE :
 * en-têtes statiques X-Client-Id / X-Api-Key (+ X-Tenant-Id), AUCUN bearer. C'est ce
 * qu'exige la Voie 1 (tâche de fond).
 *
 * Contrat vérifié via le spec OpenAPI public (/v3/api-docs) :
 *   - listing : GET /api/organizations/{organizationId}/agencies (orgId dans l'URL) ;
 *   - sécurité : en-têtes X-Client-Id + X-Api-Key ;
 *   - réponse : enveloppe { success, data:[AgencyResponse], ... } ;
 *   - AUCUNE pagination : toute la liste est renvoyée d'un coup.
 */
@Component
public class OrganizationDirectoryAdapter implements OrganizationDirectoryPort {

    private static final String AGENCIES_PATH = "/api/organizations/{organizationId}/agencies";

    private final RestClient client;

    public OrganizationDirectoryAdapter(KernelIngestionProperties props) {
        this.client = RestClient.builder()
                .baseUrl(props.api().url())
                .defaultHeader("X-Client-Id", props.api().clientId())
                .defaultHeader("X-Api-Key", props.api().apiKey())
                .defaultHeader("X-Tenant-Id", props.tenantId())
                .build();
    }

    @Override
    public List<KernelAgency> listAgencies(String organizationId, Instant since, int page, int size) {
        // L'endpoint renvoie TOUTE la liste d'un coup (pas de pagination ni de 'since').
        // On ne sert que la page 0 ; les suivantes sont vides pour clore la boucle de sync.
        if (page > 0) return List.of();

        KernelAgencyListResponse body = client.get()
                .uri(AGENCIES_PATH, organizationId)
                .retrieve()
                .body(KernelAgencyListResponse.class);

        if (body == null || body.data() == null) return List.of();

        List<KernelAgency> result = new ArrayList<>(body.data().size());
        for (KernelAgencyResponse dto : body.data()) {
            try {
                result.add(dto.toDomain());
            } catch (RuntimeException ex) {
                // agence incomplète (id ou nom manquant) → ignorée, on n'interrompt pas le sync
            }
        }
        return result;
    }
}
