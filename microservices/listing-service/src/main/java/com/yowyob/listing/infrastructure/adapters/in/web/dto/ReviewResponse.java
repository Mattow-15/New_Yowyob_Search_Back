package com.yowyob.listing.infrastructure.adapters.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {
    private UUID id;
    private Integer rating;
    private String comment;
    private String userId;
    private LocalDateTime createdAt;
}
