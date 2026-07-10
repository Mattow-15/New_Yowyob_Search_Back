package com.yowyob.search.crawler;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration du job de synchronisation Kernel → Elasticsearch.
 *
 * @param enabled        active le sync (scheduler + controller).
 * @param kernelBaseUrl  URL de base du Kernel (ex. https://kernel-core.yowyob.com).
 * @param clientId       X-Client-Id présenté au Kernel.
 * @param apiKey         X-Api-Key présenté au Kernel.
 * @param kernelTenantId X-Tenant-Id présenté au Kernel.
 * @param targetTenantId tenant sous lequel les organisations sont indexées dans Search.
 * @param scheduleCron   cron du sync automatique ("-" = désactivé).
 * @param pageSize       taille des pages lors de la pagination Kernel.
 */
@ConfigurationProperties(prefix = "kernel.sync")
public record KernelSyncProperties(
        boolean enabled,
        String kernelBaseUrl,
        String clientId,
        String apiKey,
        String kernelTenantId,
        String targetTenantId,
        String scheduleCron,
        int pageSize) {

    public KernelSyncProperties {
        if (kernelBaseUrl == null || kernelBaseUrl.isBlank()) {
            kernelBaseUrl = "https://kernel-core.yowyob.com";
        }
        kernelBaseUrl = kernelBaseUrl.stripTrailing().replaceAll("/$", "");
        if (pageSize <= 0) pageSize = 100;
        if (scheduleCron == null || scheduleCron.isBlank()) scheduleCron = "-";
    }
}
