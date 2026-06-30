package com.yowyob.config;

import com.yowyob.auth.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Configuration Spring Security pour l'application réactive (WebFlux).
 * Gère l'authentification JWT, les routes publiques/protégées et le filtre de
 * sécurité.
 *
 * @author YowYob Team
 * @since 1.0.0
 */
@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final JwtService jwtService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .authorizeExchange(exchanges -> exchanges
                        // Public endpoints
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers("/api/auth/**", "/api/v1/auth/**").permitAll()
                        .pathMatchers("/api/search/**").permitAll()
                        .pathMatchers("/api/listings/**").permitAll()
                        .pathMatchers("/api/geo/**").permitAll()
                        .pathMatchers("/actuator/**").permitAll()
                        .pathMatchers("/swagger-ui/**", "/v3/api-docs/**", "/webjars/**").permitAll()
                        // Protected endpoints
                        .pathMatchers("/api/users/**").authenticated()
                        .anyExchange().permitAll())
                .addFilterBefore(jwtWebFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint((exchange, ex) -> {
                            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                            return exchange.getResponse().setComplete();
                        }))
                .build();
    }

    @Bean
    public WebFilter jwtWebFilter() {
        return (ServerWebExchange exchange, WebFilterChain chain) -> {
            String path = exchange.getRequest().getURI().getPath();

            // Skip public routes
            if (isPublicRoute(path)) {
                return chain.filter(exchange);
            }

            String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    String userId = jwtService.extractUserId(token);
                    log.info("Extracted userId from token: {}", userId);
                    if (userId != null && jwtService.isTokenValid(token)) {
                        log.info("Token is valid. Injecting X-User-Id: {}", userId);
                        // Inject X-User-Id header for downstream processing
                        ServerWebExchange mutatedExchange = exchange.mutate()
                                .request(exchange.getRequest().mutate()
                                        .header("X-User-Id", userId)
                                        .build())
                                .build();

                        // Create Authentication object for Spring Security
                        var authorities = java.util.Collections.singletonList(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"));
                        var auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                                userId, null, authorities);

                        return chain.filter(mutatedExchange)
                                .contextWrite(org.springframework.security.core.context.ReactiveSecurityContextHolder
                                        .withAuthentication(auth));
                    } else {
                        log.warn("Token invalid or userId null");
                    }
                } catch (Exception e) {
                    log.error("Error validting token", e);
                    // Invalid token, continue without authentication
                }
            } else {
                log.debug("No valid Authorization header found");
            }

            return chain.filter(exchange);
        };
    }

    private boolean isPublicRoute(String path) {
        return path.startsWith("/api/auth")
                || path.startsWith("/api/v1/auth")
                || path.startsWith("/api/search")
                || path.startsWith("/api/listings")
                || path.startsWith("/api/geo")
                || path.startsWith("/actuator")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.endsWith("/health");
    }
}
