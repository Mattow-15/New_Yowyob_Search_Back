package com.yowyob.search.crawler;

import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Déclenchement manuel du crawl : {@code POST /api/crawler/run}. Sous {@code /api/**}, donc soumis
 * à l'auth kernel (réservé à un appelant autorisé). Présent uniquement si {@code crawler.enabled=true}.
 */
@RestController
@RequestMapping("/api/crawler")
@ConditionalOnProperty(prefix = "crawler", name = "enabled", havingValue = "true")
public class CrawlerController {

    private final CrawlerService crawlerService;

    public CrawlerController(CrawlerService crawlerService) {
        this.crawlerService = crawlerService;
    }

    @PostMapping("/run")
    public Mono<ResponseEntity<Map<String, Object>>> run() {
        return crawlerService.crawl()
                .map(count -> ResponseEntity.ok(Map.of("status", "ok", "indexed", count)));
    }
}
