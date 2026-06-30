package com.yowyob.crawling.application.ports.out;

import com.yowyob.crawling.domain.model.StandardizedDocument;

/** Port de sortie : pousse un document business book dans le pipeline (source=BUSINESS_BOOK). */
public interface BusinessBookIndexPort {
    void publish(StandardizedDocument document);
}
