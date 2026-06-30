package com.yowyob.notification.infrastructure.adapters.in.messaging;

import com.yowyob.notification.infrastructure.config.RabbitMQConfig;
import com.yowyob.notification.infrastructure.adapters.in.messaging.event.ListingEvent;
import com.yowyob.notification.application.ports.in.SendNotificationUseCase;
import com.yowyob.notification.domain.model.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ListingEventListener {

    private final SendNotificationUseCase sendNotificationUseCase;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void handleListingEvent(ListingEvent event) {
        log.info("Received Listing Event via Hexagonal Listener: {}", event.getEventType());

        if ("CREATED".equals(event.getEventType())) {
            String subject = "New Listing Alert: " + event.getTitle();
            String message = String.format("A new listing '%s' has been posted in %s for %.2f FCFA.",
                    event.getTitle(), event.getCategory(), event.getPrice());

            Notification notification = Notification.builder()
                    .recipient("users@yowyob.com")
                    .subject(subject)
                    .message(message)
                    .build();

            sendNotificationUseCase.sendNotification(notification);
        }
    }
}
