package com.yowyob.search.config;

import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration du service. {@code apiKeys} = clés acceptées (une par projet intégrateur),
 * fournies via {@code yowyob.search.api-keys} (liste). Si vide, l'authentification est DÉSACTIVÉE
 * (utile en dev/local uniquement — toujours définir des clés en prod).
 */
@ConfigurationProperties(prefix = "yowyob.search")
public class SearchProperties {

    private Set<String> apiKeys = Set.of();

    public Set<String> getApiKeys() {
        return apiKeys;
    }

    public void setApiKeys(Set<String> apiKeys) {
        this.apiKeys = apiKeys == null ? Set.of() : apiKeys;
    }

    public boolean authEnabled() {
        return !apiKeys.isEmpty();
    }
}
