package com.yowyob.search.crawler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yowyob.search.service.IndexService;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;

/**
 * Consumer réactif des événements d'organisations publiés par le Kernel sur Kafka.
 *
 * <p>Format attendu du message (valeur JSON) :
 * <pre>
 * {
 *   "eventType": "CREATED" | "UPDATED" | "DELETED",
 *   "data": {
 *     "id": "...",
 *     "name": "...",
 *     "description": "...",
 *     "website": "accounting.yowyob.com",
 *     "category": "...",
 *     "city": "...",
 *     ...
 *   }
 * }
 * </pre>
 * Si {@code eventType} est absent, le message entier est traité comme la donnée org (UPSERT).
 */
@Component
@ConditionalOnProperty(prefix = "kafka.org", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(KafkaProperties.class)
public class KafkaOrganizationConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaOrganizationConsumer.class);
    private static final String COLLECTION = "organization";
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final KafkaProperties properties;
    private final IndexService indexService;
    private final ObjectMapper mapper = new ObjectMapper();

    public KafkaOrganizationConsumer(KafkaProperties properties, IndexService indexService) {
        this.properties = properties;
        this.indexService = indexService;
    }

    @PostConstruct
    public void start() {
        if (properties.tenantId() == null || properties.tenantId().isBlank()) {
            LOGGER.warn("kafka.org.tenant-id non configuré — consumer Kafka organisations désactivé.");
            return;
        }

        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.bootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, properties.groupId());
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        // Sécurité : le broker du Kernel est en SASL_PLAINTEXT / SCRAM. Si un protocole
        // de sécurité est configuré, on ajoute les propriétés SASL (sinon PLAINTEXT par défaut).
        if (properties.securityProtocol() != null && !properties.securityProtocol().isBlank()) {
            consumerProps.put("security.protocol", properties.securityProtocol());
            if (properties.saslMechanism() != null && !properties.saslMechanism().isBlank()) {
                consumerProps.put("sasl.mechanism", properties.saslMechanism());
            }
            if (properties.saslJaasConfig() != null && !properties.saslJaasConfig().isBlank()) {
                consumerProps.put("sasl.jaas.config", properties.saslJaasConfig());
            }
        }

        ReceiverOptions<String, String> options = ReceiverOptions.<String, String>create(consumerProps)
                .subscription(java.util.List.of(properties.topic()))
                .commitInterval(Duration.ofSeconds(5))
                .commitBatchSize(10);

        LOGGER.info("Consumer Kafka organisations démarré — broker={} topic={} tenant={}",
                properties.bootstrapServers(), properties.topic(), properties.tenantId());

        KafkaReceiver.create(options)
                .receive()
                .flatMap(record -> {
                    String value = record.value();
                    return processMessage(value)
                            .doOnSuccess(v -> record.receiverOffset().acknowledge())
                            .onErrorResume(err -> {
                                LOGGER.error("Erreur traitement message Kafka org [offset={}]: {}",
                                        record.receiverOffset().offset(), err.getMessage());
                                record.receiverOffset().acknowledge(); // skip message invalide
                                return Mono.empty();
                            });
                })
                .onErrorResume(err -> {
                    LOGGER.error("Erreur fatale consumer Kafka org: {}", err.getMessage());
                    return Mono.empty();
                })
                .subscribe();
    }

    private Mono<Void> processMessage(String json) {
        Map<String, Object> parsed;
        try {
            parsed = mapper.readValue(json, MAP_TYPE);
        } catch (Exception e) {
            return Mono.error(new IllegalArgumentException("JSON invalide: " + e.getMessage()));
        }

        String eventType = parsed.containsKey("eventType")
                ? String.valueOf(parsed.get("eventType")).toUpperCase()
                : "UPSERT";

        @SuppressWarnings("unchecked")
        Map<String, Object> data = parsed.containsKey("data")
                ? (Map<String, Object>) parsed.get("data")
                : parsed;

        String id = extractId(data);
        if (id == null || id.isBlank()) {
            return Mono.error(new IllegalArgumentException("Champ 'id' manquant dans l'événement org"));
        }

        if ("DELETED".equals(eventType)) {
            return indexService.delete(properties.tenantId(), COLLECTION, id)
                    .doOnSuccess(v -> LOGGER.info("Org supprimée de l'index : {}", id))
                    .then();
        }

        // Ne pas indexer les orgs sans nom — elles pollueraient les résultats de recherche
        Object name = data.get("name");
        if (name == null || String.valueOf(name).isBlank()) {
            LOGGER.debug("Org {} ignorée : pas de nom", id);
            return Mono.empty();
        }

        Map<String, Object> doc = toIndexDocument(data);
        return indexService.index(properties.tenantId(), COLLECTION, id, doc)
                .doOnSuccess(v -> LOGGER.info("Org indexée [{}] : {}", eventType, data.get("name")))
                .then();
    }

    private Map<String, Object> toIndexDocument(Map<String, Object> data) {
        Map<String, Object> doc = new HashMap<>(data);
        doc.put("source", "KERNEL_ORG");
        doc.put("indexedAt", Instant.now().toString());
        // Normalise les champs de coordonnées si présents sous d'autres noms
        remapField(doc, "lat", "latitude");
        remapField(doc, "lon", "longitude");
        remapField(doc, "lng", "longitude");
        return doc;
    }

    private static String extractId(Map<String, Object> data) {
        for (String key : new String[]{"id", "organizationId", "orgId", "uuid"}) {
            Object v = data.get(key);
            if (v != null) return String.valueOf(v);
        }
        return null;
    }

    private static void remapField(Map<String, Object> doc, String from, String to) {
        if (doc.containsKey(from) && !doc.containsKey(to)) {
            doc.put(to, doc.remove(from));
        }
    }
}
