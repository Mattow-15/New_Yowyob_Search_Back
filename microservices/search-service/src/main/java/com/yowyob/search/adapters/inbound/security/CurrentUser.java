package com.yowyob.search.adapters.inbound.security;

import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Resout le KernelUser courant depuis le contexte de securite reactif.
 * Renvoie un Mono VIDE sur les endpoints publics (recherche anonyme) : les
 * appelants qui exigent un utilisateur doivent gerer ce cas (switchIfEmpty).
 */
@Component
public class CurrentUser {

    public Mono<KernelUser> get() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(JwtAuthenticationToken.class::isInstance)
                .cast(JwtAuthenticationToken.class)
                .map(JwtAuthenticationToken::getToken)
                .map(this::toKernelUser);
    }

    private KernelUser toKernelUser(Jwt jwt) {
        return new KernelUser(
                jwt.getSubject(),
                jwt.getClaimAsString("tid"),
                jwt.getClaimAsString("oid"),
                jwt.getClaimAsString("aid"),
                jwt.getTokenValue()
        );
    }
}
