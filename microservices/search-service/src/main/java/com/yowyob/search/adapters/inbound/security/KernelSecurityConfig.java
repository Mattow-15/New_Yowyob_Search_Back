package com.yowyob.search.adapters.inbound.security;

import com.yowyob.search.config.KernelProperties;
import com.yowyob.search.config.YowyobJwtProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Securite entrante : resource server OAuth2 reactif. Decodeur JWT resilient :
 *   - Kernel desactive  -> valide uniquement l'auth YowYob locale (HS256).
 *   - Kernel active     -> essaie d'abord le Kernel (RS256/JWKS), retombe sur
 *                          YowYob si le Kernel echoue (JWKS injoignable, ou
 *                          token emis par YowYob).
 *
 * Politique d'acces : la recherche est PUBLIQUE (CDC : recherche sans compte),
 * tout le reste (avis, historique, prefs) exige un token valide.
 */
@Configuration
@EnableWebFluxSecurity
public class KernelSecurityConfig {

    @Bean
    public ReactiveJwtDecoder jwtDecoder(
            KernelProperties kernel,
            YowyobJwtProperties yowyob,
            @Value("${kernel.auth.enabled:false}") boolean kernelEnabled) {

        SecretKey key = new SecretKeySpec(
                yowyob.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        ReactiveJwtDecoder yowyobDecoder = NimbusReactiveJwtDecoder.withSecretKey(key).build();

        if (!kernelEnabled) {
            return yowyobDecoder;
        }

        ReactiveJwtDecoder kernelDecoder =
                NimbusReactiveJwtDecoder.withJwkSetUri(kernel.jwksUri()).build();
        return token -> kernelDecoder.decode(token)
                .onErrorResume(ex -> yowyobDecoder.decode(token));
    }

    @Bean
    public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http,
                                                      ReactiveJwtDecoder jwtDecoder) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(ex -> ex
                        .pathMatchers("/actuator/**").permitAll()
                        .pathMatchers(HttpMethod.GET,
                                "/api/search/**",
                                "/api/v1/search/**",
                                "/api/v1/suggestions/**",
                                "/api/v1/services/**",
                                "/api/v1/shops/**").permitAll()
                        .anyExchange().authenticated())
                .oauth2ResourceServer(o -> o.jwt(j -> j.jwtDecoder(jwtDecoder)))
                .build();
    }
}
