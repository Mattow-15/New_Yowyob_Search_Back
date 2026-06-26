package com.yowyob.search.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Authentification déléguée au kernel : yowyob-search ne tient plus de liste de clés statique.
 * Chaque appel présente {@code X-Client-Id} + {@code X-Api-Key} d'un clientApplication du kernel,
 * que yowyob-search valide en appelant {@code GET /api/client-applications/me} du kernel. Un
 * clientApplication créé dans le kernel fonctionne donc immédiatement, sans aucun rechargement.
 */
@ConfigurationProperties(prefix = "yowyob.search.auth")
public class KernelAuthProperties {

    /** Si false : auth désactivée (DEV uniquement). */
    private boolean enabled = true;
    /** URL de base du kernel (interne au réseau Docker). */
    private String kernelBaseUrl = "http://kernel-core-kernel-layer-1:8080";
    /** Durée (secondes) pendant laquelle un couple (clientId, apiKey) validé reste en cache. */
    private long cacheTtlSeconds = 300;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getKernelBaseUrl() {
        return kernelBaseUrl;
    }

    public void setKernelBaseUrl(String kernelBaseUrl) {
        this.kernelBaseUrl = kernelBaseUrl;
    }

    public long getCacheTtlSeconds() {
        return cacheTtlSeconds;
    }

    public void setCacheTtlSeconds(long cacheTtlSeconds) {
        this.cacheTtlSeconds = cacheTtlSeconds;
    }
}
