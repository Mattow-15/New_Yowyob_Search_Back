package com.yowyob.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for product search result with distance information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductWithDistance {
    private String id;
    private String name;
    private String description;
    private String type;
    private String city;
    private Double latitude;
    private Double longitude;
    private Double distanceKm; // Distance from user's location
    private java.util.List<String> images;
}
