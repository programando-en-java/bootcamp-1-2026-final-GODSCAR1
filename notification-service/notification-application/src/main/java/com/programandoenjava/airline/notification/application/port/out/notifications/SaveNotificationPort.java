package com.programandoenjava.airline.notification.application.port.out.notifications;

import com.programandoenjava.airline.notification.domain.notification.Notification;

public interface SaveNotificationPort {

    void save(Notification notification);

    /** Records that the channel took it, once the channel has. */
    void markSent(Notification notification);
}
