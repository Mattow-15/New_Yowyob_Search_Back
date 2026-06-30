package com.yowyob.auth.infrastructure.adapters.out.messaging;

import com.yowyob.auth.application.ports.out.UserEventPublisherPort;
import com.yowyob.auth.domain.model.User;
import com.yowyob.auth.infrastructure.adapters.in.messaging.event.UserCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaUserPublisherAdapter implements UserEventPublisherPort {

    // KafkaTemplate<String, Object> : correspond au bean fourni par KafkaProducerConfig.
    // (Spring 7 exige une correspondance générique stricte à l'injection.)
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC = "user.events";

    @Override
    @Async
    public void publishUserCreated(User user) {
        log.info("Preparing to send UserCreatedEvent to Kafka for user: {}", user.getEmail());
        UserCreatedEvent event = UserCreatedEvent.builder()
                .id(user.getId().toString())
                .email(user.getEmail())
                .username(user.getName())
                .firstName(user.getName())
                .build();
        try {
            kafkaTemplate.send(TOPIC, event.getId(), event);
            log.info("Successfully sent event for user: {}", event.getEmail());
        } catch (Exception e) {
            log.error("Failed to send event to Kafka: {}", e.getMessage(), e);
        }
    }
}
