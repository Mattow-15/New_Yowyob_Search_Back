package com.yowyob.listing.application.ports.in;

import com.yowyob.listing.domain.model.Listing;

/**
 * PORT ENTRANT — Traitement d'une annonce provenant du crawler (via Kafka).
 * Le KafkaListener dépendra de cette interface.
 * Contient la logique d'upsert idempotente (créer ou mettre à jour selon osmId).
 */
public interface ProcessScrapedListingUseCase {

    /**
     * Traite une annonce scrapée : crée ou met à jour l'enregistrement
     * selon l'osmId, puis publie un événement vers RabbitMQ.
     *
     * @param listing l'annonce construite à partir du message Kafka
     * @return l'annonce persistée (créée ou mise à jour)
     */
    Listing execute(Listing listing);
}
