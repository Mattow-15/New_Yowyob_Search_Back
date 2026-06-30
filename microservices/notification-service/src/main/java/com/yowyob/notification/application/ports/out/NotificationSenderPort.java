package com.yowyob.notification.application.ports.out;

import com.yowyob.notification.domain.model.Notification;

public interface NotificationSenderPort {
    void send(Notification notification);
}
