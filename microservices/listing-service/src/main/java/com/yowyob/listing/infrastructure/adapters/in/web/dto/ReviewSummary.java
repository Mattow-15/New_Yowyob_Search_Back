package com.yowyob.listing.infrastructure.adapters.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewSummary {
    private Double averageRating;
    private Integer reviewCount;
    private Map<Integer, Long> ratingDistribution; // ex: {1:2, 2:0, 3:5, 4:12, 5:8}
}
