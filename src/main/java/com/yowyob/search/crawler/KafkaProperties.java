package com.yowyob.search.crawler;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration du consumer Kafka pour les événements d'organisations du Kernel.
 *
 * @param enabled     active le consumer (désactivé si le broker n'est pas disponible).
 * @param bootstrapServers adresse du broker Kafka (ex: kafka:9092).
 * @param topic       topic d'événements d'organisations publié par le Kernel.
 * @param groupId     consumer group ID (doit être unique par instance de yowyob-search).
 * @param tenantId    tenant sous lequel les organisations sont indexées dans ES.
 */
@ConfigurationProperties(prefix = "kafka.org")
public record KafkaProperties(
        boolean enabled,
        String bootstrapServers,
        String topic,
        String groupId,
        String tenantId) {

    public KafkaProperties {
        if (bootstrapServers == null || bootstrapServers.isBlank()) {
            bootstrapServers = "kafka:9092";
        }
        if (topic == null || topic.isBlank()) {
            topic = "organization.events";
        }
        if (groupId == null || groupId.isBlank()) {
            groupId = "yowyob-search-org-consumer";
        }
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = "";
        }
    }
}
