package com.yowyob.crawling.infrastructure.config;

import com.yowyob.crawling.application.services.BusinessBookSyncService;
import com.yowyob.crawling.application.ports.out.BusinessBookIndexPort;
import com.yowyob.crawling.application.ports.out.StandardizedDocumentSourcePort;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(BusinessBookProperties.class)
public class BusinessBookConfig {

    @Bean
    public BusinessBookSyncService businessBookSyncService(StandardizedDocumentSourcePort source,
                                                           BusinessBookIndexPort index,
                                                           BusinessBookProperties props) {
        return new BusinessBookSyncService(source, index, props.serviceName(), props.pageSize());
    }
}
