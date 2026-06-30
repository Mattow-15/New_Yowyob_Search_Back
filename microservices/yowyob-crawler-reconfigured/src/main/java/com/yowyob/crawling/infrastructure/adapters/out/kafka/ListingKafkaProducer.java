package com.yowyob.crawling.infrastructure.adapters.out.kafka;

import com.yowyob.crawling.domain.model.SearchableService;
import com.yowyob.crawling.application.ports.out.ServicePublishedPort;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ListingKafkaProducer implements ServicePublishedPort {

    @Value("${kafka.topics.listings}")
    private String topic;

    private final KafkaTemplate<String, ListingEvent> kafkaTemplate;

    @Override
    public void publish(SearchableService service) {
        ListingEvent event = toEvent(service);
        
        kafkaTemplate.send(topic, event.getOsmId(), event)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Échec Kafka pour {} : {}", event.getName(), ex.getMessage());
                } else {
                    log.debug("Publié sur Kafka : {}", event.getName());
                }
            });
    }

    /**
     * Point de couture pour les composants qui construisent leur propre ListingEvent
     * (ex: KernelOrgListingPublisher). Réutilise le même topic et le même KafkaTemplate
     * — un seul producteur pour toutes les sources.
     */
    public void publishEvent(ListingEvent event) {
        kafkaTemplate.send(topic, event.getOsmId(), event)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Échec Kafka (Kernel) pour {} : {}", event.getName(), ex.getMessage());
                } else {
                    log.debug("Kernel publié sur Kafka : {}", event.getName());
                }
            });
    }

    private ListingEvent toEvent(SearchableService service) {
        return ListingEvent.builder()
            .osmId(service.getId())
            .name(service.getName())
            .address(service.getAddress())
            .street(service.getStreet())
            .latitude(service.getLocation() != null ? service.getLocation().latitude() : 0.0)
            .longitude(service.getLocation() != null ? service.getLocation().longitude() : 0.0)
            .phone(service.getPhone())
            .website(service.getWebsite())
            .openingHours(service.getOpeningHours())
            .openNow(service.getOpenNow())
            .category(service.getCategory())
            .imageUrl(service.getImageUrl())
            .rating(service.getRating())
            .reviewCount(service.getReviewCount())
            .reviewsSummary(service.getReviewsSummary())
            .priceLevel(service.getPriceLevel())
            .googleMapsUrl(service.getGoogleMapsUrl())
            .sourceCity(service.getCity())
            .crawledAt(service.getCrawledAt())
            .source(service.getSource())
            .build();
    }

    // ── DTO local pour la publication Kafka ────────────────────────
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListingEvent {
        private String osmId;
        private String name;
        private String address;
        private String street;
        private Double latitude;
        private Double longitude;
        private String phone;
        private String website;
        private String openingHours;
        private Boolean openNow;
        private String category;
        private String imageUrl;
        private Double rating;
        private Integer reviewCount;
        private String reviewsSummary;
        private Integer priceLevel;
        private String googleMapsUrl;
        private String sourceCity;
        private String crawledAt;
        private String source;
    }
}
