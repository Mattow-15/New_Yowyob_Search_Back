package com.yowyob.search.config;

import java.nio.charset.StandardCharsets;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Authentifie les appels {@code /api/**} par clé d'API (en-tête {@code X-Api-Key}). Les endpoints
 * d'observabilité ({@code /actuator/**}) restent ouverts. Si aucune clé n'est configurée, le filtre
 * laisse passer (mode dev — ne jamais déployer sans clés).
 */
@Configuration
@EnableConfigurationProperties(SearchProperties.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiKeyWebFilter implements WebFilter {

    private static final String API_KEY_HEADER = "X-Api-Key";

    private final SearchProperties properties;

    public ApiKeyWebFilter(SearchProperties properties) {
        this.properties = properties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().pathWithinApplication().value();
        if (!path.startsWith("/api/") || !properties.authEnabled()) {
            return chain.filter(exchange);
        }
        String provided = exchange.getRequest().getHeaders().getFirst(API_KEY_HEADER);
        if (provided != null && properties.getApiKeys().contains(provided)) {
            return chain.filter(exchange);
        }
        return unauthorized(exchange.getResponse());
    }

    private Mono<Void> unauthorized(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body = "{\"error\":\"Invalid or missing X-Api-Key\"}".getBytes(StandardCharsets.UTF_8);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
    }
}
