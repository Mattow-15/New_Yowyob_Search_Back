package com.yowyob.listing.application.ports.out;

import com.yowyob.listing.domain.model.Listing;

/**
 * PORT SORTANT — Contrat de publication d'événements.
 *
 * Le domaine dit "publie cet événement" sans savoir que c'est RabbitMQ
 * qui le recevra. Si demain on passe à Kafka ou SNS, seul l'adaptateur change.
 */
public interface ListingEventPublisherPort {

    void publishListingCreated(Listing listing);

    void publishListingUpdated(Listing listing);

    void publishListingDeleted(Listing listing);
}
