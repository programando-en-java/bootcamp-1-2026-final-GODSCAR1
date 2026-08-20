package com.programandoenjava.airline.notification.infrastructure.adapter.out.channel;

import com.programandoenjava.airline.notification.application.port.out.channel.NotificationChannel;
import com.programandoenjava.airline.notification.domain.notification.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The channel this system actually has. No passenger in it holds an email
 * address, a phone number or a name, so there is nowhere to send anything: what
 * this does is write down what would have been sent, and to whom (ADR-017).
 *
 * <p>The row in notifications is what a test reads. This log line is what a
 * person reads while watching the stack run.
 */
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
