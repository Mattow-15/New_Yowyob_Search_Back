package com.yowyob.crawling.infrastructure.adapters.out.kernel;

import com.yowyob.crawling.infrastructure.adapters.out.kafka.ListingKafkaProducer;
import com.yowyob.crawling.domain.model.GeoPoint;
import com.yowyob.crawling.domain.model.KernelAgency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class KernelOrgListingPublisherTest {

    private ListingKafkaProducer producer;
    private KernelOrgListingPublisher publisher;

    @BeforeEach
    void setUp() {
        producer = mock(ListingKafkaProducer.class);
        publisher = new KernelOrgListingPublisher(producer);
    }

    @Test
    void shouldMapAndPublishKernelAgencyCorrectly() {
        // Given
        KernelAgency agency = new KernelAgency(
                "agency-123",
                "org-456",
                "Agence Centrale",
                "RESTAURANT",
                new GeoPoint(3.87, 11.52),
                "Mvan, Yaoundé",
                "Yaoundé",
                "+237699999999",
                "mvan@agency.com",
                "8h-18h",
                Instant.now(),
                null // imageUrl
        );

        // When
        publisher.publish(agency);

        // Then
        ArgumentCaptor<ListingKafkaProducer.ListingEvent> captor = ArgumentCaptor.forClass(ListingKafkaProducer.ListingEvent.class);
        verify(producer, times(1)).publishEvent(captor.capture());

        ListingKafkaProducer.ListingEvent event = captor.getValue();
        assertNotNull(event);
        assertEquals("agency-123", event.getOsmId());
        assertEquals("Agence Centrale", event.getName());
        assertEquals("RESTAURANT", event.getCategory());
        assertEquals("Mvan, Yaoundé", event.getAddress());
        assertEquals("Yaoundé", event.getSourceCity());
        assertEquals(3.87, event.getLatitude());
        assertEquals(11.52, event.getLongitude());
        assertEquals("+237699999999", event.getPhone());
        assertEquals("8h-18h", event.getOpeningHours());
        assertEquals("KERNEL_ORG", event.getSource());
        assertNotNull(event.getCrawledAt());

        // Vérification des valeurs par défaut pour les champs absents
        assertNull(event.getImageUrl());
        assertNull(event.getWebsite());
        assertNull(event.getOpenNow());
        assertNull(event.getRating());
        assertNull(event.getReviewCount());
        assertNull(event.getReviewsSummary());
        assertNull(event.getPriceLevel());
        assertNull(event.getGoogleMapsUrl());
    }
}
