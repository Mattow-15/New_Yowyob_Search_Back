package com.yowyob.crawling.infrastructure.config;

import com.yowyob.crawling.application.services.KernelOrgSyncService;
import com.yowyob.crawling.application.ports.out.KernelOrgIndexPort;
import com.yowyob.crawling.application.ports.out.OrganizationDirectoryPort;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Cablage de l'ingestion Kernel (Voie 1). Seul endroit qui connait Spring + le scheduling. */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(KernelIngestionProperties.class)
public class KernelIngestionConfig {

    @Bean
    public KernelOrgSyncService kernelOrgSyncService(OrganizationDirectoryPort directory,
                                                     KernelOrgIndexPort index,
                                                     KernelIngestionProperties props) {
        return new KernelOrgSyncService(directory, index, props.organizationIds(), props.pageSize());
    }
}
