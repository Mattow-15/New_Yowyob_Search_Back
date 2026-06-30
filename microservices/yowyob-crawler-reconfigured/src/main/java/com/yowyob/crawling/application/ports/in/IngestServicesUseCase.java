package com.yowyob.crawling.application.ports.in;

import com.yowyob.crawling.domain.model.IngestCommand;
import com.yowyob.crawling.domain.model.IngestReport;

public interface IngestServicesUseCase {
    IngestReport ingest(IngestCommand command);
}
