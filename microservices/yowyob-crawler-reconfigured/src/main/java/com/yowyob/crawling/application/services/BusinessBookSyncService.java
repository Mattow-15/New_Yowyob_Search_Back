package com.yowyob.crawling.application.services;

import com.yowyob.crawling.domain.model.StandardizedDocument;
import com.yowyob.crawling.application.ports.out.BusinessBookIndexPort;
import com.yowyob.crawling.application.ports.out.StandardizedDocumentSourcePort;

import java.time.Instant;
import java.util.List;
import java.util.logging.Logger;

/** Pull incrémental paginé des documents business book vers l'index. Java pur. */
public class BusinessBookSyncService {

    private static final Logger log = Logger.getLogger(BusinessBookSyncService.class.getName());

    private final StandardizedDocumentSourcePort source;
    private final BusinessBookIndexPort index;
    private final String serviceName;
    private final int pageSize;

    public BusinessBookSyncService(StandardizedDocumentSourcePort source,
                                   BusinessBookIndexPort index,
                                   String serviceName,
                                   int pageSize) {
        this.source = source;
        this.index = index;
        this.serviceName = serviceName;
        this.pageSize = pageSize <= 0 ? 100 : Math.min(pageSize, 500);
    }

    /** @return nombre de documents publiés. */
    public int sync(Instant since) {
        int published = 0;
        int page = 1; // le contrat est 1-based
        while (true) {
            List<StandardizedDocument> batch = source.fetch(serviceName, since, page, pageSize);
            if (batch == null || batch.isEmpty()) break;
            for (StandardizedDocument doc : batch) {
                try {
                    index.publish(doc);
                    published++;
                } catch (RuntimeException ex) {
                    log.warning("Publication doc " + doc.id() + " échouée: " + ex.getMessage());
                }
            }
            if (batch.size() < pageSize) break;
            page++;
        }
        int total = published;
        log.info(() -> "Sync business book terminé : " + total + " documents publiés");
        return published;
    }
}
