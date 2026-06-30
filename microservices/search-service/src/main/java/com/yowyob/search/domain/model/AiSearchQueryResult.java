package com.yowyob.search.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiSearchQueryResult {
    private String aiAnswer;
    private String intent;
    private String rewrittenQuery;
    private List<Product> sources;
    private List<String> subQueries;
    private long processingTimeMs;
    private boolean aiMode;
}
