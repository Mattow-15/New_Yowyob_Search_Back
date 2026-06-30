package com.yowyob.crawling.infrastructure.adapters.out.kernel.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.yowyob.crawling.domain.model.GeoPoint;
import com.yowyob.crawling.domain.model.KernelAgency;

import java.util.List;

/**
 * DTO de transport : forme RÉELLE d'une agence renvoyée par organization-core
 * (schéma OpenAPI {@code AgencyResponse} de kernel-core.yowyob.com, vérifié via /v3/api-docs).
 *
 * On ne déclare que les champs exploités pour l'indexation ; le reste est ignoré
 * ({@code @JsonIgnoreProperties}). La liste est enveloppée dans {@link KernelAgencyListResponse}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KernelAgencyResponse(
        String id,
        String organizationId,
        String name,
        String shortName,
        String longName,
        String agencyType,
        String description,
        String location,   // localisation en texte libre
        String city,
        String country,
        Double latitude,
        Double longitude,
        String openTime,
        String closeTime,
        String phone,
        String email,
        List<String> keywords,
        String logoUri,
        // ── Champs de gouvernance pour le filtre d'indexation (cf. isIndexable) ──
        Boolean active,
        @JsonProperty("isPublic") Boolean isPublic,
        @JsonProperty("isHeadquarter") Boolean isHeadquarter,
        String deletedAt   // horodatage ISO si l'agence est supprimée, null sinon
) {
    /**
     * RÈGLE D'OR (moteur PUBLIC) : on n'indexe qu'une agence active, publique,
     * non supprimée et géolocalisée. Jamais de commerce privé/supprimé dans l'index,
     * jamais d'agence sans coordonnées (inutilisable pour la recherche de proximité).
     */
    public boolean isIndexable() {
        return Boolean.TRUE.equals(active)
                && Boolean.TRUE.equals(isPublic)
                && (deletedAt == null || deletedAt.isBlank())
                && latitude != null
                && longitude != null;
    }

    public KernelAgency toDomain() {
        GeoPoint geo = (latitude != null && longitude != null)
                ? new GeoPoint(latitude, longitude) : null;

        String resolvedName = firstNonBlank(name, shortName, longName);
        // businessDomain → category ES : on prend le type d'agence, sinon le 1er mot-clé.
        String domain = firstNonBlank(
                agencyType,
                (keywords != null && !keywords.isEmpty()) ? keywords.get(0) : null,
                "Agence");
        String address = firstNonBlank(location, joinNonBlank(", ", city, country));
        String hours = (notBlank(openTime) && notBlank(closeTime))
                ? openTime + " - " + closeTime : null;

        // updatedAt = null : l'endpoint n'expose pas de date de mise à jour de contenu
        // (governedAt = gouvernance, pas le contenu) ; la synchro incrémentale reste désactivée.
        return new KernelAgency(id, organizationId, resolvedName, domain,
                geo, address, city, phone, email, hours, null,
                notBlank(logoUri) ? logoUri : null);
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (notBlank(v)) return v;
        }
        return null;
    }

    private static String joinNonBlank(String sep, String... values) {
        StringBuilder sb = new StringBuilder();
        for (String v : values) {
            if (notBlank(v)) {
                if (sb.length() > 0) sb.append(sep);
                sb.append(v);
            }
        }
        return sb.length() == 0 ? null : sb.toString();
    }
}
