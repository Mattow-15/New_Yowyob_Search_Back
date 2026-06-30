package com.yowyob.geo.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeocodeLocation {
    private String address;
    private double latitude;
    private double longitude;
}
