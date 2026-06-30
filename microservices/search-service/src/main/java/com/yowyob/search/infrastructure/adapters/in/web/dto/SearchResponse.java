package com.yowyob.search.infrastructure.adapters.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResponse {
    private Boolean success;
    private String query;
    private Integer total;
    private List<ProductDto> results;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductDto {
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
        private List<String> images;
        private String imageUrl;
        private String phone;
        private String website;
        private Double latitude;
        private Double longitude;
        
        // Google Places fields
        private String street;
        private Integer reviewsCount;
        private Boolean openNow;
        private String openingHours;
        private Integer priceLevel;
        private String reviewsSummary;
        private String googleMapsUrl;

        // Provenance du document ES (ex: "google_places", "KERNEL_ORG", "WEB").
        // Permet au front d'afficher un badge selon la source (ex: "Annuaire officiel").
        private String source;

        // Proximity info
        private Double distanceKm;

        // Link to full listing details
        public String getDetailsUrl() {
            return "/api/search/" + id + "/details";
        }
    }
}