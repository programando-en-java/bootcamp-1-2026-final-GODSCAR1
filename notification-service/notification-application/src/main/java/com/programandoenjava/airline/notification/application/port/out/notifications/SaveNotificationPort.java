package com.programandoenjava.airline.notification.application.port.out.notifications;

import com.programandoenjava.airline.notification.domain.notification.Notification;

public interface SaveNotificationPort {

    void save(Notification notification);

    void markSent(Notification notification);
}
