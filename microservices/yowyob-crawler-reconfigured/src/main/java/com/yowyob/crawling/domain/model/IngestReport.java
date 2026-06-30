package com.yowyob.crawling.domain.model;

public record IngestReport(
    ServiceSource sourceUsed,
    int totalProcessed,
    int totalEnriched
) {}
