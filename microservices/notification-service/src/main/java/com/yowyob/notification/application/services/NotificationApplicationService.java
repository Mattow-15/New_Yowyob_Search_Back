package com.yowyob.notification.application.services;

import com.yowyob.notification.application.ports.in.SendNotificationUseCase;
import com.yowyob.notification.application.ports.out.NotificationSenderPort;
import com.yowyob.notification.domain.model.Notification;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class NotificationApplicationService implements SendNotificationUseCase {

    private final NotificationSenderPort notificationSenderPort;

    @Override
    public void sendNotification(Notification notification) {
        notificationSenderPort.send(notification);
    }
}
