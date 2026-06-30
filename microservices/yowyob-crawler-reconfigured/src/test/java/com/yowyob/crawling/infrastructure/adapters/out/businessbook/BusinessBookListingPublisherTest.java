package com.yowyob.crawling.infrastructure.adapters.out.businessbook;

import com.yowyob.crawling.infrastructure.adapters.out.kafka.ListingKafkaProducer;
import com.yowyob.crawling.domain.model.StandardizedDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BusinessBookListingPublisherTest {

    private ListingKafkaProducer producer;
    private BusinessBookListingPublisher publisher;

    @BeforeEach
    void setUp() {
        producer = mock(ListingKafkaProducer.class);
        publisher = new BusinessBookListingPublisher(producer);
    }

    @Test
    void shouldMapAndPublishStandardizedDocumentCorrectly() {
        // Given
        Map<String, Object> content = new HashMap<>();
        content.put("latitude", 3.8480);
        content.put("longitude", 11.5021);
        content.put("address", "Mvan, Yaoundé");
        content.put("phone", "+237600000000");
        content.put("website", "https://businessbook.example.com");
        content.put("openingHours", "Mon-Fri 8:00 AM - 6:00 PM");
        content.put("openNow", true);
        content.put("rating", 4.5);
        content.put("reviewsCount", 24);
        content.put("city", "Yaoundé");
        content.put("imageUrl", "https://image.example.com/logo.png");

        StandardizedDocument doc = new StandardizedDocument(
                "doc-999",
                "SERVICES_RESTAURANTS",
                "Le Bon Resto",
                "Un excellent restaurant traditionnel",
                content,
                List.of("resto", "camerounais"),
                Instant.now(),
                Instant.now()
        );

        // When
        publisher.publish(doc);

        // Then
        ArgumentCaptor<ListingKafkaProducer.ListingEvent> captor = ArgumentCaptor.forClass(ListingKafkaProducer.ListingEvent.class);
        verify(producer, times(1)).publishEvent(captor.capture());

        ListingKafkaProducer.ListingEvent event = captor.getValue();
        assertNotNull(event);
        assertEquals("doc-999", event.getOsmId());
        assertEquals("Le Bon Resto", event.getName());
        assertEquals("SERVICES_RESTAURANTS", event.getCategory());
        assertEquals("Mvan, Yaoundé", event.getAddress());
        assertEquals("Mvan, Yaoundé", event.getStreet());
        assertEquals(3.8480, event.getLatitude());
        assertEquals(11.5021, event.getLongitude());
        assertEquals("+237600000000", event.getPhone());
        assertEquals("https://businessbook.example.com", event.getWebsite());
        assertEquals("Mon-Fri 8:00 AM - 6:00 PM", event.getOpeningHours());
        assertTrue(event.getOpenNow());
        assertEquals(4.5, event.getRating());
        assertEquals(24, event.getReviewCount());
        assertEquals("https://image.example.com/logo.png", event.getImageUrl());
        assertEquals("Yaoundé", event.getSourceCity());
        assertEquals("BUSINESS_BOOK", event.getSource());
        assertNotNull(event.getCrawledAt());
    }

    @Test
    void shouldHandleNullOrMalformedContentFieldsGracefully() {
        // Given
        StandardizedDocument doc = new StandardizedDocument(
                "doc-empty",
                "SERVICES_PHARMACIES",
                "Pharmacie du Centre",
                "Pharmacie de garde",
                null,
                List.of(),
                Instant.now(),
                Instant.now()
        );

        // When
        publisher.publish(doc);

        // Then
        ArgumentCaptor<ListingKafkaProducer.ListingEvent> captor = ArgumentCaptor.forClass(ListingKafkaProducer.ListingEvent.class);
        verify(producer, times(1)).publishEvent(captor.capture());

        ListingKafkaProducer.ListingEvent event = captor.getValue();
        assertNotNull(event);
        assertEquals("doc-empty", event.getOsmId());
        assertEquals("Pharmacie du Centre", event.getName());
        assertEquals("SERVICES_PHARMACIES", event.getCategory());
        assertNull(event.getAddress());
        assertNull(event.getStreet());
        assertNull(event.getLatitude());
        assertNull(event.getLongitude());
        assertNull(event.getPhone());
        assertNull(event.getWebsite());
        assertNull(event.getOpeningHours());
        assertNull(event.getOpenNow());
        assertNull(event.getRating());
        assertNull(event.getReviewCount());
        assertNull(event.getImageUrl());
        assertNull(event.getSourceCity());
        assertEquals("BUSINESS_BOOK", event.getSource());
        assertNotNull(event.getCrawledAt());
    }
}
