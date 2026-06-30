package com.yowyob.listing.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO mapping the incoming data from the Crawler Service.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScrapedListingDto {
    private String source;
    private String externalId;
    private String title;
    private String description;
    private Double price;
    private String currency;
    private String city;
    private String country;
    private String category;
    private String url;
    private String imageUrl;

    // Geolocation
    private Double latitude;
    private Double longitude;

    private LocalDateTime scrapedAt;
}
