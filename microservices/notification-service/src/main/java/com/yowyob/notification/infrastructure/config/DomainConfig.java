package com.yowyob.notification.infrastructure.config;

import com.yowyob.notification.application.ports.out.NotificationSenderPort;
import com.yowyob.notification.application.services.NotificationApplicationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainConfig {

    @Bean
    public NotificationApplicationService notificationApplicationService(
            NotificationSenderPort notificationSenderPort) {
        return new NotificationApplicationService(notificationSenderPort);
    }
}
