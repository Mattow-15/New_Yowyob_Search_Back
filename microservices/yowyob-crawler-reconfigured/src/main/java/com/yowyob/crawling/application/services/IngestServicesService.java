package com.yowyob.crawling.application.services;

import com.yowyob.crawling.domain.model.*;
import com.yowyob.crawling.application.ports.in.IngestServicesUseCase;
import com.yowyob.crawling.application.ports.out.*;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.Optional;

@Slf4j
public class IngestServicesService implements IngestServicesUseCase {

    private final List<ServiceSourcePort> sourcePorts;
    private final PhotoProviderPort photoProviderPort;
    private final ServicePublishedPort publishedPort;

    public IngestServicesService(
            List<ServiceSourcePort> sourcePorts,
            PhotoProviderPort photoProviderPort,
            ServicePublishedPort publishedPort) {
        this.sourcePorts = sourcePorts;
        this.photoProviderPort = photoProviderPort;
        this.publishedPort = publishedPort;
    }

    @Override
    public IngestReport ingest(IngestCommand command) {
        log.info("Démarrage de l'ingestion. Source préférée : {}", command.preferredSource());
        
        ServiceSourcePort activeSource = selectBestSource(command.preferredSource());
        log.info("Source sélectionnée pour l'ingestion : {}", activeSource.getSourceType());

        int totalProcessed = 0;
        int totalEnriched = 0;

        for (IngestCommand.Target target : command.targets()) {
            log.info("Début du crawl pour cible [Ville: {}, Type: {}, Rayon: {}m]",
                    target.cityName(), target.placeType(), target.radiusMeters());

            try {
                List<RawService> rawServices = activeSource.fetch(
                    target.cityName(),
                    target.lat(),
                    target.lng(),
                    target.radiusMeters(),
                    target.placeType()
                );

                log.info("Récupéré {} services bruts depuis {}", rawServices.size(), activeSource.getSourceType());

                for (RawService raw : rawServices) {
                    try {
                        Optional<String> photo = photoProviderPort.findPhotoUrl(raw);
                        if (photo.isPresent()) {
                            totalEnriched++;
                        }
                        
                        SearchableService searchable = SearchableService.from(raw, photo.orElse(null));
                        publishedPort.publish(searchable);
                        totalProcessed++;
                    } catch (Exception e) {
                        log.error("Erreur lors du traitement/enrichissement du service '{}' : {}", raw.getName(), e.getMessage(), e);
                    }
                }
            } catch (Exception e) {
                log.error("Échec du crawl de la cible {} : {}", target.cityName(), e.getMessage(), e);
            }
        }

        log.info("Ingestion complétée. Total traités : {}, Total enrichis : {}", totalProcessed, totalEnriched);
        return new IngestReport(activeSource.getSourceType(), totalProcessed, totalEnriched);
    }

    private ServiceSourcePort selectBestSource(ServiceSource preferred) {
        return sourcePorts.stream()
            .filter(port -> port.getSourceType() == preferred && port.isAvailable())
            .findFirst()
            .orElseGet(() -> sourcePorts.stream()
                .filter(ServiceSourcePort::isAvailable)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Aucune source de crawling n'est disponible et active")));
    }
}
