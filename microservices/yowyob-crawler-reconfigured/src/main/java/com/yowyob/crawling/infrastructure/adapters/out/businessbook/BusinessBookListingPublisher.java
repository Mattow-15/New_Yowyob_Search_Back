package com.yowyob.crawling.infrastructure.adapters.out.businessbook;

import com.yowyob.crawling.infrastructure.adapters.out.kafka.ListingKafkaProducer;
import com.yowyob.crawling.domain.model.StandardizedDocument;
import com.yowyob.crawling.application.ports.out.BusinessBookIndexPort;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

/**
 * Mappe un StandardizedDocument -> ListingEvent (source=BUSINESS_BOOK) et le
 * publie via le ListingKafkaProducer EXISTANT. Même topic, même producteur que
 * web et Kernel.
 */
@Component
public class BusinessBookListingPublisher implements BusinessBookIndexPort {

    private final ListingKafkaProducer producer;

    public BusinessBookListingPublisher(ListingKafkaProducer producer) {
        this.producer = producer;
    }

    @Override
    public void publish(StandardizedDocument doc) {
        Map<String, Object> content = doc.content();

        // Extraction des coordonnées GPS
        Double lat = getDouble(content, "latitude");
        if (lat == null) {
            lat = getDouble(content, "lat");
        }
        Double lng = getDouble(content, "longitude");
        if (lng == null) {
            lng = getDouble(content, "lng");
        }

        // Extraction des autres champs
        String address = getString(content, "address");
        String street = getString(content, "street");
        if (street == null) {
            street = address;
        }

        String phone = getString(content, "phone");
        if (phone == null) {
            phone = getString(content, "phoneNumber");
        }

        String website = getString(content, "website");
        String openingHours = getString(content, "openingHours");
        Boolean openNow = getBoolean(content, "openNow");
        
        Double rating = getDouble(content, "rating");
        Integer reviewCount = getInteger(content, "reviewsCount");
        if (reviewCount == null) {
            reviewCount = getInteger(content, "reviewCount");
        }
        String reviewsSummary = getString(content, "reviewsSummary");
        Integer priceLevel = getInteger(content, "priceLevel");
        String imageUrl = getString(content, "imageUrl");
        if (imageUrl == null) {
            imageUrl = getString(content, "image");
        }
        String googleMapsUrl = getString(content, "googleMapsUrl");
        String sourceCity = getString(content, "city");

        ListingKafkaProducer.ListingEvent event = ListingKafkaProducer.ListingEvent.builder()
                .osmId(doc.id())
                .name(doc.title())
                .category(doc.entity())
                .address(address)
                .street(street)
                .latitude(lat)
                .longitude(lng)
                .phone(phone)
                .website(website)
                .openingHours(openingHours)
                .openNow(openNow)
                .rating(rating)
                .reviewCount(reviewCount)
                .reviewsSummary(reviewsSummary)
                .priceLevel(priceLevel)
                .imageUrl(imageUrl)
                .googleMapsUrl(googleMapsUrl)
                .sourceCity(sourceCity)
                .source("BUSINESS_BOOK")
                .crawledAt(Instant.now().toString())
                .build();

        producer.publishEvent(event);
    }

    private Double getDouble(Map<String, Object> map, String key) {
        if (map == null) return null;
        Object val = map.get(key);
        if (val instanceof Number) {
            return ((Number) val).doubleValue();
        }
        if (val instanceof String) {
            try {
                return Double.parseDouble((String) val);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private Integer getInteger(Map<String, Object> map, String key) {
        if (map == null) return null;
        Object val = map.get(key);
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        if (val instanceof String) {
            try {
                return Integer.parseInt((String) val);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private Boolean getBoolean(Map<String, Object> map, String key) {
        if (map == null) return null;
        Object val = map.get(key);
        if (val instanceof Boolean) {
            return (Boolean) val;
        }
        if (val instanceof String) {
            return Boolean.parseBoolean((String) val);
        }
        return null;
    }

    private String getString(Map<String, Object> map, String key) {
        if (map == null) return null;
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }
}
