package com.yowyob.listing.infrastructure.adapters.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Miroir fidele du contrat publie par le crawler sur le topic crawler.listings.events
 * (cf. ListingKafkaProducer.ListingEvent cote yowyob-crawler-reconfigured).
 * Toute evolution du contrat doit etre faite ici ET la -- ou mieux, un jour, dans un
 * module commun partage.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrawlerListingRequest {
    private String  osmId;
    private String  name;
    private String  address;
    private String  street;
    private Double  latitude;
    private Double  longitude;
    private String  phone;
    private String  website;
    private String  openingHours;
    private Boolean openNow;
    private String  category;
    private String  imageUrl;
    private Double  rating;
    private Integer reviewCount;
    private String  reviewsSummary;
    private Integer priceLevel;
    private String  googleMapsUrl;
    private String  sourceCity;
    private String  crawledAt;
    private String  source;
}
