package com.yowyob.listing.infrastructure.adapters.out.messaging;

import com.yowyob.listing.application.ports.out.ListingEventPublisherPort;
import com.yowyob.listing.domain.model.Listing;
import com.yowyob.listing.infrastructure.adapters.in.messaging.event.ListingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * ADAPTATEUR SORTANT — RabbitMQ.
 *
 * Implémente ListingEventPublisherPort. C'est ici ET UNIQUEMENT ICI que
 * l'on sait que le système de messagerie est RabbitMQ.
 *
 * Responsabilité critique : mapper le Listing (domaine) → ListingEvent (DTO
 * partagé avec le search-service). Le contrat du ListingEvent ne doit pas
 * changer sans coordination avec le search-service (piège n°3 du plan).
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RabbitMQListingPublisherAdapter implements ListingEventPublisherPort {

    private static final String EXCHANGE_NAME = "listing.events";

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publishListingCreated(Listing listing) {
        publish(listing, "CREATED");
    }

    @Override
    public void publishListingUpdated(Listing listing) {
        publish(listing, "UPDATED");
    }

    @Override
    public void publishListingDeleted(Listing listing) {
        publish(listing, "DELETED");
    }

    // ─── MAPPING À LA MAIN : Domaine → DTO événement ─────────────────────────
    // Ce bloc est le seul endroit où l'on sait comment le Listing du domaine
    // se traduit en ListingEvent pour le search-service.

    private void publish(Listing listing, String eventType) {
        ListingEvent event = toEvent(listing, eventType);
        String routingKey = "listing." + eventType.toLowerCase();
        log.info("Publishing ListingEvent type={} id={} routing={}", eventType, listing.getId(), routingKey);
        rabbitTemplate.convertAndSend(EXCHANGE_NAME, routingKey, event);
    }

    private ListingEvent toEvent(Listing listing, String eventType) {
        return ListingEvent.builder()
                .id(listing.getId())
                .title(listing.getTitle())
                .description(listing.getDescription())
                .price(listing.getPrice())
                .category(listing.getCategory())
                .address(listing.getAddress())
                .latitude(listing.getLatitude())
                .longitude(listing.getLongitude())
                .imageUrl(listing.getImageUrl())
                .phone(listing.getPhone())
                .website(listing.getWebsite())
                .openingHours(listing.getOpeningHours())
                .rating(listing.getRating())
                .reviewsCount(listing.getReviewsCount())
                .status(listing.getStatus() != null ? listing.getStatus().name() : null)
                .sellerId(listing.getSellerId())
                .source(listing.getSource())
                .eventType(eventType)
                .build();
    }
}
