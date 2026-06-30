package com.yowyob.crawling.domain.model;

import lombok.Builder;
import lombok.Value;
import java.time.Instant;

@Value
@Builder(toBuilder = true)
public class SearchableService {
    String id;
    String name;
    String address;
    String street;
    String city;
    GeoPoint location;
    String phone;
    String website;
    String category;
    Double rating;
    Integer reviewCount;
    String reviewsSummary;
    String openingHours;
    Boolean openNow;
    Integer priceLevel;
    String googleMapsUrl;
    String imageUrl;
    String source;
    String crawledAt;

    public static SearchableService from(RawService raw, String imageUrl) {
        return SearchableService.builder()
            .id(raw.getRawId())
            .name(raw.getName())
            .address(raw.getAddress())
            .street(raw.getStreet())
            .city(raw.getCity())
            .location(raw.getLocation())
            .phone(raw.getPhone())
            .website(raw.getWebsite())
            .category(raw.getCategory())
            .rating(raw.getRating())
            .reviewCount(raw.getReviewCount())
            .reviewsSummary(raw.getReviewsSummary())
            .openingHours(raw.getOpeningHours())
            .openNow(raw.getOpenNow())
            .priceLevel(raw.getPriceLevel())
            .googleMapsUrl(raw.getGoogleMapsUrl())
            .imageUrl(imageUrl)
            .source(raw.getSource().name().toLowerCase())
            .crawledAt(Instant.now().toString())
            .build();
    }
}
