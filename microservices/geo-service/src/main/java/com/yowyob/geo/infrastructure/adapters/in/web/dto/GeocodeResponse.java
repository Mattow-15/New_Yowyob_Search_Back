package com.yowyob.geo.infrastructure.adapters.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeocodeResponse implements Serializable {
    private String address;
    private Double latitude;
    private Double longitude;
}
