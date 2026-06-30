package com.yowyob.crawling.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** Config de l'ingestion Kernel (prefixe "kernel.ingestion"). */
@ConfigurationProperties(prefix = "kernel.ingestion")
public record KernelIngestionProperties(Api api, String tenantId,
                                        List<String> organizationIds, int pageSize) {

    public record Api(String url, String clientId, String apiKey) {}

    public KernelIngestionProperties {
        if (pageSize <= 0) pageSize = 100;
        // Le listing des agences est SCOPE PAR ORGANISATION (V3 : code agence unique par org,
        // organization-core = source de verite par tenant+org). On boucle donc sur une liste
        // d'organizationId ; une liste a 1 element couvre le cas "une seule organisation".
        if (organizationIds == null) organizationIds = List.of();
    }
}
