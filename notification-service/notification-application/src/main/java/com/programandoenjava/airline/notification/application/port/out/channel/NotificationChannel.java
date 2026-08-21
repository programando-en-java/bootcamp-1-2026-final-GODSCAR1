package com.programandoenjava.airline.notification.application.port.out.channel;

import com.programandoenjava.airline.notification.domain.notification.Notification;

public interface NotificationChannel {

    void send(Notification notification);
}
