package com.yowyob.crawling.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Document du contrat d'API standardisé inter-services
 * (GET /api/{service}/search/documents). Modèle de domaine pur.
 * 'content' est générique : sa forme dépend du service producteur ;
 * c'est le mapping vers ListingEvent qui la connaît.
 */
public record StandardizedDocument(
        String id,
        String entity,
        String title,
        String description,
        Map<String, Object> content,
        List<String> tags,
        Instant createdAt,
        Instant updatedAt
) {}
