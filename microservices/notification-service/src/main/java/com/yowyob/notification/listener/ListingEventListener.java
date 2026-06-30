package com.yowyob.notification.listener;

import com.yowyob.notification.config.RabbitMQConfig;
import com.yowyob.notification.event.ListingEvent;
import com.yowyob.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ListingEventListener {

    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void handleListingEvent(ListingEvent event) {
        log.info("Received Listing Event: {}", event.getEventType());

        if ("CREATED".equals(event.getEventType())) {
            String subject = "New Listing Alert: " + event.getTitle();
            String message = String.format("A new listing '%s' has been posted in %s for %.2f FCFA.",
                    event.getTitle(), event.getCategory(), event.getPrice());

            // Simulate sending to "all subscribed users"
            notificationService.sendNotification("users@yowyob.com", subject, message);
        }
    }
}
