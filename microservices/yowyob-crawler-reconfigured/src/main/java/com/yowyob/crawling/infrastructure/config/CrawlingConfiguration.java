package com.yowyob.crawling.infrastructure.config;

import com.yowyob.crawling.application.services.IngestServicesService;
import com.yowyob.crawling.application.ports.in.IngestServicesUseCase;
import com.yowyob.crawling.application.ports.out.PhotoProviderPort;
import com.yowyob.crawling.application.ports.out.ServicePublishedPort;
import com.yowyob.crawling.application.ports.out.ServiceSourcePort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class CrawlingConfiguration {

    @Bean
    public IngestServicesUseCase ingestServicesUseCase(
            List<ServiceSourcePort> sourcePorts,
            PhotoProviderPort photoProviderPort,
            ServicePublishedPort publishedPort) {
        return new IngestServicesService(sourcePorts, photoProviderPort, publishedPort);
    }
}
