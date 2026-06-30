package com.yowyob.search.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IpLocation {
    private String city;
    private String country;
    private Double latitude;
    private Double longitude;
}
