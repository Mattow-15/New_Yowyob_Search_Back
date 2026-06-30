package com.yowyob.crawling.application.ports.out;

import com.yowyob.crawling.domain.model.StandardizedDocument;
import java.time.Instant;
import java.util.List;

/**
 * Port de sortie : lecture d'un service producteur respectant le contrat de pull
 * standardisé. Générique : un seul port/adaptateur pour business book ET toute
 * future source interne. Le 'serviceName' sélectionne le producteur.
 */
public interface StandardizedDocumentSourcePort {

    List<StandardizedDocument> fetch(String serviceName, Instant since, int page, int size);
}
