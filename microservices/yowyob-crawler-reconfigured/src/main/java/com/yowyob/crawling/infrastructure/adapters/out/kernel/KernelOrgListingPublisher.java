package com.yowyob.crawling.infrastructure.adapters.out.kernel;

import com.yowyob.crawling.infrastructure.adapters.out.kafka.ListingKafkaProducer;
import com.yowyob.crawling.domain.model.KernelAgency;
import com.yowyob.crawling.application.ports.out.KernelOrgIndexPort;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Adaptateur : mappe une agence Kernel vers le ListingEvent du crawler existant
 * et le publie sur le même topic Kafka (crawler.listings.events) avec source = KERNEL_ORG.
 * Ainsi l'agence suit EXACTEMENT le même chemin que les commerces crawlés :
 *   listing-service (CrawlerListingKafkaListener) → PostgreSQL → RabbitMQ → search-service → ES.
 *
 * Point de couture : réutilise ListingKafkaProducer#publishEvent — UN SEUL producteur
 * pour toutes les sources (web crawl + Kernel).
 *
 * Champs obligatoires pour que l'agence soit cherchable et géolocalisable :
 *   - name     → title    (terme de recherche principal)
 *   - location → lat/lng  (geo_point ES, proximité)
 *   - businessDomain → category (facette ES)
 *   - city     → sourceCity (filtre géographique)
 *
 * Champs absents pour une agence : price, imageUrl → null (aucune contrainte NOT-NULL en aval).
 */
@Component
public class KernelOrgListingPublisher implements KernelOrgIndexPort {

    private final ListingKafkaProducer producer;

    public KernelOrgListingPublisher(ListingKafkaProducer producer) {
        this.producer = producer;
    }

    @Override
    public void publish(KernelAgency agency) {
        ListingKafkaProducer.ListingEvent event = ListingKafkaProducer.ListingEvent.builder()
                // Clé de déduplication : l'id Kernel sert d'osmId (même champ, même rôle)
                .osmId(agency.id())
                // Champs cherchables
                .name(agency.name())
                .category(agency.businessDomain())
                .address(agency.address())
                .street(agency.address())           // pas de champ street séparé côté Kernel
                .sourceCity(agency.city())
                // Géolocalisation — champs prioritaires pour la recherche par proximité ES
                .latitude(agency.location() != null ? agency.location().latitude() : null)
                .longitude(agency.location() != null ? agency.location().longitude() : null)
                // Contact
                .phone(agency.phone())
                .openingHours(agency.openingHours())
                // Logo de l'agence (logoUri du Kernel) → vignette sur la fiche
                .imageUrl(agency.imageUrl())
                // Champs absents pour une agence (null → pas de contrainte NOT-NULL en aval)
                .website(null)
                .openNow(null)
                .rating(null)
                .reviewCount(null)
                .reviewsSummary(null)
                .priceLevel(null)
                .googleMapsUrl(null)
                // Traçabilité
                .source("KERNEL_ORG")
                .crawledAt(Instant.now().toString())
                .build();

        producer.publishEvent(event);
    }
}
