package com.yowyob.notification.listener;

import com.yowyob.config.RabbitMQConfig;
import com.yowyob.listing.event.ListingEvent;
import com.yowyob.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Listener RabbitMQ pour les événements listing côté notification.
 * Envoie une notification email lors de la création d'une nouvelle annonce.
 *
 * @author YowYob Team
 * @since 1.0.0
 */
@Component("notificationListingEventListener")
@Slf4j
@RequiredArgsConstructor
public class NotificationListingEventListener {

    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void handleListingEvent(ListingEvent event) {
        log.info("Received Listing Event: {}", event.getEventType());

        if ("CREATED".equals(event.getEventType())) {
            String subject = "New Listing Alert: " + event.getTitle();
            String message = String.format("A new listing '%s' has been posted in %s for %.2f FCFA.",
                    event.getTitle(), event.getCategory(), event.getPrice());

            notificationService.sendNotification("users@yowyob.com", subject, message);
        }
    }
}
