package com.yowyob.crawling.infrastructure.adapters.out.businessbook.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StandardizedResponse(
        String service,
        Instant timestamp,
        Pagination pagination,
        List<StandardizedDocumentDto> documents
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Pagination(int page, int size, int totalPages, long totalElements) {}
}
