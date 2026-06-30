package com.yowyob.search.infrastructure.adapters.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for geographical coordinates returned by GeoService
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GeoLocationDto {
    private Double latitude;
    private Double longitude;
    private String city;
    private String country;
}
