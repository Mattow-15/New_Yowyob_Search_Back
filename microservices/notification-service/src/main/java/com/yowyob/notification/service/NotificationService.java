package com.yowyob.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationService {

    public void sendNotification(String recipient, String subject, String message) {
        // Simulation of sending email
        log.info("--------------------------------------------------");
        log.info("SENDING NOTIFICATION (Simulated)");
        log.info("To: {}", recipient);
        log.info("Subject: {}", subject);
        log.info("Message: {}", message);
        log.info("--------------------------------------------------");
    }
}
