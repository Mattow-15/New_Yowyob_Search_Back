package com.yowyob.crawling.application.ports.out;

import com.yowyob.crawling.domain.model.KernelAgency;

/**
 * Port de sortie : pousse une agence Kernel dans le pipeline d'indexation
 * (meme entree que le crawler web : topic listings.raw, source = KERNEL_ORG).
 * L'application ne sait pas si derriere c'est Kafka, RabbitMQ ou autre.
 */
public interface KernelOrgIndexPort {
    void publish(KernelAgency agency);
}
