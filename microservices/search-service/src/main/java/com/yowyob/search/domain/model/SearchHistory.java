package com.yowyob.search.domain.model;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SearchHistory {
    private String id;
    private String userId;
    private String query;
    private String type;
    private String city;
    private Instant timestamp;
}
