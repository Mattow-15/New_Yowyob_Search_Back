package com.yowyob.crawling.domain.model;

import java.time.Instant;

/**
 * Agence lue depuis organization-core (source de verite du Kernel).
 * Modele de domaine pur. Les champs reprennent ce que le rapport decrit :
 * shortName/longName, email, coordonnees GPS (via Address), horaires, domaine
 * d'activite (BusinessDomain). Reutilise le GeoPoint deja present dans le crawler.
 */
public record KernelAgency(
        String id,
        String organizationId,
        String name,
        String businessDomain,
        GeoPoint location,
        String address,
        String city,
        String phone,
        String email,
        String openingHours,
        Instant updatedAt,
        String imageUrl
) {
    public KernelAgency {
        if (id == null || id.isBlank())
            throw new IllegalArgumentException("id d'agence obligatoire");
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("nom d'agence obligatoire");
    }
}
