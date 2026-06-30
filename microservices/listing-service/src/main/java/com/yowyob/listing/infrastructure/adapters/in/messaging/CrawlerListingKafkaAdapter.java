package com.yowyob.listing.infrastructure.adapters.in.messaging;

import com.yowyob.listing.application.ports.in.ProcessScrapedListingUseCase;
import com.yowyob.listing.domain.model.Listing;
import com.yowyob.listing.domain.model.ListingStatus;
import com.yowyob.listing.infrastructure.adapters.in.web.dto.CrawlerListingRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * ADAPTATEUR ENTRANT — Kafka.
 *
 * Rôle unique : recevoir le message Kafka, le convertir en objet
 * du domaine, et déléguer au port entrant ProcessScrapedListingUseCase.
 * AUCUNE logique métier ici. Ce n'est que de la traduction.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CrawlerListingKafkaAdapter {

    private static final UUID SYSTEM_CRAWLER_SELLER =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final ProcessScrapedListingUseCase processScrapedListingUseCase;

    @KafkaListener(
            topics = "${kafka.topics.listings}",
            groupId = "listing-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onCrawlerListing(CrawlerListingRequest req,
                                 @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                 @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("Received crawler listing topic={} offset={} osmId={} source={}",
                topic, offset, req.getOsmId(), req.getSource());
        try {
            // Traduction du message Kafka → modèle du domaine
            Listing listing = Listing.builder()
                    .osmId(req.getOsmId())
                    .externalId(req.getOsmId())
                    .title(req.getName())
                    .description("Source: " + (req.getSource() != null ? req.getSource() : "UNKNOWN"))
                    .price(0.0)
                    .category(req.getCategory() != null ? req.getCategory() : "GENERAL")
                    .address(joinAddress(req.getAddress(), req.getSourceCity()))
                    .latitude(req.getLatitude())
                    .longitude(req.getLongitude())
                    .imageUrl(req.getImageUrl())
                    .phone(req.getPhone())
                    .website(req.getWebsite())
                    .openingHours(req.getOpeningHours())
                    .rating(req.getRating())
                    .reviewsCount(req.getReviewCount())
                    .status(ListingStatus.ACTIVE)
                    .sellerId(SYSTEM_CRAWLER_SELLER)
                    .build();

            // Délégation au domaine — toute la logique d'upsert est là-bas
            processScrapedListingUseCase.execute(listing);
            log.info("Listing processed -- osmId={} source={}", req.getOsmId(), req.getSource());

        } catch (Exception e) {
            log.error("Failed to process listing osmId={} source={} -- reason: {}",
                    req.getOsmId(), req.getSource(), e.getMessage(), e);
            throw e;
        }
    }

    private String joinAddress(String address, String sourceCity) {
        if (address == null || address.isBlank()) return sourceCity;
        if (sourceCity == null || sourceCity.isBlank()) return address;
        return address + ", " + sourceCity;
    }
}
