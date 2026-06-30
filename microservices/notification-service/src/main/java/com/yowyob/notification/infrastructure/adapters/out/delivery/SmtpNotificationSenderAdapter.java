package com.yowyob.notification.infrastructure.adapters.out.delivery;

import com.yowyob.notification.application.ports.out.NotificationSenderPort;
import com.yowyob.notification.domain.model.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SmtpNotificationSenderAdapter implements NotificationSenderPort {

    @Override
    public void send(Notification notification) {
        log.info("--------------------------------------------------");
        log.info("SENDING NOTIFICATION (Simulated via SMTP Adapter)");
        log.info("To: {}", notification.getRecipient());
        log.info("Subject: {}", notification.getSubject());
        log.info("Message: {}", notification.getMessage());
        log.info("--------------------------------------------------");
    }
}
