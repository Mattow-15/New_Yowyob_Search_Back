package com.yowyob.crawling.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.List;

@ConfigurationProperties(prefix = "crawler")
public record CrawlerProperties(
    List<CityConfig> cities,
    List<String> osmTypes,
    ScheduleConfig schedule
) {
    public record CityConfig(
        String name,
        double lat,
        double lng,
        int radiusMeters
    ) {}

    public record ScheduleConfig(
        String cron
    ) {}
}
