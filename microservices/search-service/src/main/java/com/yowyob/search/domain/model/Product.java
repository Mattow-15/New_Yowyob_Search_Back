package com.yowyob.search.domain.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    private String id;
    private String title;
    private String description;
    private Double price;
    private String serviceType;
    private String type;
    private String category;
    private String city;
    private String quartier;
    private Double rating;
    private Double latitude;
    private Double longitude;
    private List<String> images;
    private String imageUrl;
    private String phone;
    private String website;
    private String openingHours;
    private Integer reviewsCount;
    private Boolean openNow;
    private Integer priceLevel;
    private String reviewsSummary;
    private String googleMapsUrl;
    private String street;
    private String source;
    private float[] embedding;
}
