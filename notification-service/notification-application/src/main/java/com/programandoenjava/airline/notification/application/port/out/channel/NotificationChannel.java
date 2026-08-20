package com.programandoenjava.airline.notification.application.port.out.channel;

import com.programandoenjava.airline.notification.domain.notification.Notification;

/**
 * Where a notification goes. A port rather than a call, because what is behind
 * it today writes to a log and what would be behind it in an airline is an
 * email or an SMS provider, and nothing above this line should have to change
 * for that (ADR-017).
 */
public interface NotificationChannel {

    void send(Notification notification);
}
