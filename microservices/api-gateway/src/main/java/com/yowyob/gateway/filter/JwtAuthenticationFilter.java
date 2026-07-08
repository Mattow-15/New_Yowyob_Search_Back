package com.yowyob.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.util.List;
import java.util.function.Predicate;

@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${kernel.jwks-uri:}")
    private String kernelJwksUri;

    private volatile NimbusJwtDecoder kernelDecoder;

    public JwtAuthenticationFilter() {
        super(Config.class);
    }

    private NimbusJwtDecoder kernelDecoder() {
        if (kernelDecoder == null && kernelJwksUri != null && kernelJwksUri.startsWith("http")) {
            synchronized (this) {
                if (kernelDecoder == null) {
                    kernelDecoder = NimbusJwtDecoder.withJwkSetUri(kernelJwksUri).build();
                }
            }
        }
        return kernelDecoder;
    }

    private String extractUserId(String token) {
        // 1. Essai Kernel (RS256 via JWKS)
        NimbusJwtDecoder decoder = kernelDecoder();
        if (decoder != null) {
            try {
                Jwt jwt = decoder.decode(token);
                return jwt.getSubject();
            } catch (Exception ignored) {
                // Kernel indisponible ou token local → fallback
            }
        }
        // 2. Fallback local HS256
        try {
            return extractAllClaims(token).getSubject();
        } catch (Exception ignored) {
            return null;
        }
    }

    public static class Config {
        // Configuration parameters if needed
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            String path = request.getURI().getPath();

            System.out.println("Processing request: " + request.getMethod() + " " + path);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                if (isPublicRoute.test(request)) {
                    System.out.println("Public route accessed without token: " + path);
                    return chain.filter(exchange);
                } else {
                    System.out.println("Private route accessed without token: " + path);
                    return onError(exchange, HttpStatus.UNAUTHORIZED);
                }
            }

            String token = authHeader.substring(7);
            String userId = extractUserId(token);

            if (userId == null) {
                if (isPublicRoute.test(request)) {
                    return chain.filter(exchange);
                } else {
                    return onError(exchange, HttpStatus.UNAUTHORIZED);
                }
            }

            ServerHttpRequest modifiedRequest = exchange.getRequest()
                    .mutate()
                    .header("X-User-Id", userId)
                    .build();

            return chain.filter(exchange.mutate().request(modifiedRequest).build());
        };
    }

    private final Predicate<ServerHttpRequest> isPublicRoute = request -> {
        List<String> publicEndpoints = List.of(
                "/api/auth/login",
                "/api/auth/register",
                "/api/auth/google",
                "/api/auth/refresh",
                "/api/auth/health",
                "/api/search",
                "/api/search/health",
                "/api/search/proximity",
                "/api/search/near-me",
                "/api/geo/health",
                "/api/geo/geocode",
                "/api/geo/distance",
                "/api/geo/ip-location",
                "/api/crawler");

        if (request.getMethod() == HttpMethod.GET) {
            String path = request.getURI().getPath();
            if (path.contains("/api/listings") || path.contains("/api/products")) {
                return true;
            }
        }

        return publicEndpoints.stream().anyMatch(uri -> request.getURI().getPath().contains(uri));
    };

    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }

    private Claims extractAllClaims(String token) {
        byte[] keyBytes = secret.getBytes();
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
