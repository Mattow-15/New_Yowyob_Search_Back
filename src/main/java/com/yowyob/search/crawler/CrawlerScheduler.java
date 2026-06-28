package com.yowyob.search.crawler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Crawl automatique planifié. Le cron vient de {@code crawler.schedule-cron} ; la valeur spéciale
 * {@code "-"} (défaut) désactive la planification (crawl alors uniquement manuel via le controller).
 */
@Component
@ConditionalOnProperty(prefix = "crawler", name = "enabled", havingValue = "true")
public class CrawlerScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrawlerScheduler.class);

    private final CrawlerService crawlerService;

    public CrawlerScheduler(CrawlerService crawlerService) {
        this.crawlerService = crawlerService;
    }

    @Scheduled(cron = "${crawler.schedule-cron:-}")
    public void scheduledCrawl() {
        LOGGER.info("Crawl planifié déclenché.");
        crawlerService.crawl()
                .subscribe(
                        count -> LOGGER.info("Crawl planifié terminé : {} document(s).", count),
                        error -> LOGGER.error("Crawl planifié en échec: {}", error.toString()));
    }
}
