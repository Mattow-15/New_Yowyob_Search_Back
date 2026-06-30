package com.yowyob.geo.infrastructure.adapters.in.web.dto;

import lombok.Data;

@Data
public class DistanceRequest {
    private Double lat1;
    private Double lon1;
    private Double lat2;
    private Double lon2;
}
