package com.yowyob.search.adapters.inbound.security;

/**
 * Contexte utilisateur extrait du JWT valide emis par le Kernel.
 * Claims : sub (utilisateur), tid (tenant), oid (organisation), aid (agence).
 * bearerToken = token brut, a relayer tel quel vers le Kernel.
 */
public record KernelUser(
        String userId,
        String tenantId,
        String organizationId,
        String agencyId,
        String bearerToken
) {}
