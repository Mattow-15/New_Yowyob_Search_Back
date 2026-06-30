package com.yowyob.auth.service;

import com.yowyob.auth.event.UserCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Kafka producer service - currently a no-op since the crawler is kept as a
 * separate service.
 * If Kafka integration is re-enabled later, replace with actual KafkaTemplate.
 */
@Service
@Slf4j
public class KafkaProducerService {

    public void sendUserCreatedEvent(UserCreatedEvent event) {
        log.info("User created event (no-op, crawler is separate): {}", event.getEmail());
    }
}
