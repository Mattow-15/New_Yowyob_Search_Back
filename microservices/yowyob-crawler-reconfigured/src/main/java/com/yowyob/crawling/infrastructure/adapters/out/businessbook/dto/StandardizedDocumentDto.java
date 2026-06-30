package com.yowyob.crawling.infrastructure.adapters.out.businessbook.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.yowyob.crawling.domain.model.StandardizedDocument;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StandardizedDocumentDto(
        String id,
        String entity,
        String title,
        String description,
        Map<String, Object> content,
        List<String> tags,
        Instant createdAt,
        Instant updatedAt
) {
    public StandardizedDocument toDomain() {
        return new StandardizedDocument(id, entity, title, description,
                content, tags, createdAt, updatedAt);
    }
}
