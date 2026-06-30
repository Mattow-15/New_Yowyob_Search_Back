package com.yowyob.search.listener;

import com.yowyob.config.RabbitMQConfig;
import com.yowyob.listing.event.ListingEvent;
import com.yowyob.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Listener RabbitMQ pour les événements listing côté recherche.
 * Indexe les nouvelles annonces dans Elasticsearch pour la recherche full-text.
 *
 * @author YowYob Team
 * @since 1.0.0
 */
@Component("searchListingEventListener")
@Slf4j
@RequiredArgsConstructor
public class SearchListingEventListener {

    private final SearchService searchService;

    @RabbitListener(queues = RabbitMQConfig.SEARCH_QUEUE)
    public void handleListingEvent(ListingEvent event) {
        log.info("Received Listing Event: {} for Listing ID: {}", event.getEventType(), event.getId());

        if ("DELETED".equals(event.getEventType())) {
            log.info("Skipping delete for now (not implemented in SearchService yet)");
        } else {
            com.yowyob.search.document.ProductDocument doc = new com.yowyob.search.document.ProductDocument();
            doc.setId(event.getId().toString());
            doc.setTitle(event.getTitle());
            doc.setDescription(event.getDescription());
            doc.setPrice(event.getPrice());
            doc.setCategory(event.getCategory());
            doc.setCity(event.getAddress());
            doc.setServiceType("LISTING");
            doc.setRating(0.0);

            searchService.indexProduct(doc).subscribe(
                    result -> log.info("Successfully indexed listing: {}", result.getId()),
                    error -> log.error("Error indexing listing: {}", error.getMessage()));
        }
    }
}
