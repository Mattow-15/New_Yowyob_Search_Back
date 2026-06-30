package com.yowyob.search.infrastructure.adapters.in.messaging.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ListingEvent {

    private String id;
    private String eventType;
    private String title;
    private String description;
    private Double price;
    private String category;
    private String address;
    private String street;
    private Double latitude;
    private Double longitude;
    private String imageUrl;
    private Double rating;
    private Integer reviewsCount;
    private String phone;
    private String website;
    private String openingHours;

    // ── Nouveaux champs Google Places ─────────────────────────────
    private Boolean openNow;
    private Integer priceLevel;
    private String reviewsSummary;
    private String googleMapsUrl;
    private String source;
}
