package com.yowyob.crawling.infrastructure.adapters.out.businessbook;

import com.yowyob.crawling.infrastructure.config.BusinessBookProperties;
import org.springframework.stereotype.Component;

/**
 * Renvoie un token s2s lu dans la config (env var).
 * À REMPLACER à terme par un vrai provider client-credentials qui appelle le
 * service d'authentification central et met le token en cache jusqu'à expiration.
 * Le port isole ce détail : le reste du code ne changera pas.
 */
@Component
public class ConfiguredServiceTokenProvider implements ServiceTokenProvider {

    private final String token;

    public ConfiguredServiceTokenProvider(BusinessBookProperties props) {
        this.token = props.serviceToken();
    }

    @Override
    public String getToken() {
        return token;
    }
}
