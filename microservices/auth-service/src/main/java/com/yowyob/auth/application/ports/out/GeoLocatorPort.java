package com.yowyob.auth.application.ports.out;

import com.yowyob.auth.domain.model.GeoLocation;

import java.util.Optional;

/**
 * Port sortant : résout la position géographique d'une adresse IP.
 * Implémenté par un adaptateur d'infrastructure (ex. ip-api.com).
 */
public interface GeoLocatorPort {

    /**
     * @param ipAddress IP du client (peut être null/locale).
     * @return la position si elle a pu être déterminée, sinon {@link Optional#empty()}.
     *         Ne lève jamais d'exception : la géoloc ne doit pas bloquer la connexion.
     */
    Optional<GeoLocation> locate(String ipAddress);
}
