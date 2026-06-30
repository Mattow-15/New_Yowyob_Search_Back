package com.yowyob.crawling.domain.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class RawService {
    String rawId;
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
    ServiceSource source;
}
