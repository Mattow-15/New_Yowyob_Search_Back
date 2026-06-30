package com.yowyob.listing.infrastructure.adapters.in.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ListingEvent implements Serializable {
    private UUID id;
    private String title;
    private String description;
    private Double price;
    private String category;
    private String address;
    private String street;
    private Double latitude;
    private Double longitude;
    private String status;
    private UUID sellerId;
    private String eventType; // CREATED, UPDATED, DELETED
    
    private String imageUrl;
    private String phone;
    private String website;
    private String openingHours;
    private Double rating;
    private Integer reviewsCount;
}
