package com.yowyob.listing.application.ports.out;

import com.yowyob.listing.domain.model.Listing;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * PORT SORTANT — Contrat de persistance vu par le domaine.
 *
 * RÈGLE D'OR : toutes les méthodes manipulent uniquement des objets
 * du domaine (Listing). Aucune trace de JPA, Hibernate ou Spring Data.
 * L'adaptateur PostgreSQL implémentera ce contrat et s'occupera du mapping.
 */
public interface ListingRepositoryPort {

    Listing save(Listing listing);

    Optional<Listing> findById(UUID id);

    /** Utilisé pour l'upsert idempotent du crawler */
    Optional<Listing> findByOsmId(String osmId);

    List<Listing> findAll();

    List<Listing> findBySellerId(UUID sellerId);

    List<Listing> findByUpdatedAtAfter(LocalDateTime dateTime);

    void delete(Listing listing);
}
