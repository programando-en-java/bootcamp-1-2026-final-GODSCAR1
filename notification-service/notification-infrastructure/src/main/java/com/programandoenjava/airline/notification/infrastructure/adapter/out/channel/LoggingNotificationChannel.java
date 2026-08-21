package com.programandoenjava.airline.notification.infrastructure.adapter.out.channel;

import com.programandoenjava.airline.notification.application.port.out.channel.NotificationChannel;
import com.programandoenjava.airline.notification.domain.notification.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class LoggingNotificationChannel implements NotificationChannel {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingNotificationChannel.class);

    @Override
    public void send(final Notification notification) {
        LOGGER.info("To passenger {} about booking {}: {} | {}",
                notification.passengerId().value(),
                notification.bookingId().value(),
                notification.message().subject(),
                notification.message().body());
    }
}
