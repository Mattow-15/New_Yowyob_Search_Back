package com.yowyob.crawling.domain.model;

import java.util.List;

public record IngestCommand(
    ServiceSource preferredSource,
    List<Target> targets
) {
    public record Target(
        String cityName,
        double lat,
        double lng,
        int radiusMeters,
        String placeType
    ) {}
}
