package com.yowyob.crawling.infrastructure.adapters.in.scheduler;

import com.yowyob.crawling.infrastructure.config.CrawlerProperties;
import com.yowyob.crawling.domain.model.IngestCommand;
import com.yowyob.crawling.domain.model.IngestReport;
import com.yowyob.crawling.domain.model.ServiceSource;
import com.yowyob.crawling.application.ports.in.IngestServicesUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class CrawlerScheduler {

    private final IngestServicesUseCase ingestUseCase;
    private final CrawlerProperties props;

    @Scheduled(cron = "${crawler.schedule.cron}")
    public void runCrawl() {
        log.info("===== DÉBUT CRAWL PÉRIODIQUE (HEXAGONAL) =====");

        List<IngestCommand.Target> targets = new ArrayList<>();
        if (props.cities() != null && props.osmTypes() != null) {
            for (CrawlerProperties.CityConfig city : props.cities()) {
                for (String type : props.osmTypes()) {
                    targets.add(new IngestCommand.Target(
                        city.name(),
                        city.lat(),
                        city.lng(),
                        city.radiusMeters(),
                        type
                    ));
                }
            }
        }

        if (targets.isEmpty()) {
            log.warn("Aucune cible de crawling n'est configurée.");
            return;
        }

        try {
            // Par défaut, on préfère GOOGLE, l'orchestrateur fera le repli si indisponible
            IngestCommand command = new IngestCommand(ServiceSource.GOOGLE, targets);
            IngestReport report = ingestUseCase.ingest(command);
            
            log.info("Crawl complété. Source utilisée : {}, Total traités : {}, Enrichis : {}",
                report.sourceUsed(), report.totalProcessed(), report.totalEnriched());
        } catch (Exception e) {
            log.error("Erreur critique durant le crawl périodique : {}", e.getMessage(), e);
        }

        log.info("===== FIN CRAWL PÉRIODIQUE =====");
    }
}
