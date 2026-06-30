package com.yowyob.search.infrastructure.adapters.in.messaging;

import com.yowyob.search.application.ports.in.SearchUseCase;
import com.yowyob.search.application.ports.out.EmbeddingClientPort;
import com.yowyob.search.config.RabbitMQConfig;
import com.yowyob.search.domain.model.Product;
import com.yowyob.search.infrastructure.adapters.in.messaging.event.ListingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ListingEventListenerAdapter {

    private final SearchUseCase searchUseCase;
    private final EmbeddingClientPort embeddingClient;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void handleListingEvent(ListingEvent event) {
        log.info("Received ListingEvent: {} for ID: {}", event.getEventType(), event.getId());

        if ("DELETED".equals(event.getEventType())) {
            log.info("Delete non implémenté");
            return;
        }

        Product product = Product.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .price(event.getPrice())
                .category(event.getCategory())
                .city(event.getAddress())
                .street(event.getStreet())
                .serviceType("LISTING")
                .imageUrl(event.getImageUrl())
                .rating(event.getRating())
                .reviewsCount(event.getReviewsCount())
                .phone(event.getPhone())
                .website(event.getWebsite())
                .openingHours(event.getOpeningHours())
                .latitude(event.getLatitude())
                .longitude(event.getLongitude())
                .openNow(event.getOpenNow())
                .priceLevel(event.getPriceLevel())
                .reviewsSummary(event.getReviewsSummary())
                .googleMapsUrl(event.getGoogleMapsUrl())
                .source(event.getSource())
                .build();

        String textToEmbed = embeddingClient.buildTextToEmbed(
                event.getTitle(),
                event.getDescription(),
                event.getCategory(),
                event.getAddress()
        );

        embeddingClient.embed(textToEmbed)
                .defaultIfEmpty(new float[0])
                .flatMap(vector -> {
                    if (vector.length > 0) {
                        product.setEmbedding(vector);
                    }
                    return searchUseCase.indexProduct(product);
                })
                .subscribe(
                        result -> log.info("Indexé : {}", result.getTitle()),
                        error -> log.error("Erreur indexation : {}", error.getMessage())
                );
    }
}
