package com.yowyob.notification.application.ports.in;

import com.yowyob.notification.domain.model.Notification;

public interface SendNotificationUseCase {
    void sendNotification(Notification notification);
}
