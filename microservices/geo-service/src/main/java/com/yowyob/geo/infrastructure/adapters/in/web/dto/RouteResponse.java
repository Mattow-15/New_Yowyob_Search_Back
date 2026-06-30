package com.yowyob.geo.infrastructure.adapters.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteResponse {
    private double distance; // in meters
    private double duration; // in seconds
    private String polyline; // Encoded polyline or JSON string of points
}
