package com.programandoenjava.airline.notification.application.usecase;

import com.programandoenjava.airline.notification.application.port.out.notifications.SaveNotificationPort;
import com.programandoenjava.airline.notification.application.port.out.processedevents.ProcessedEventsPort;
import com.programandoenjava.airline.notification.application.transaction.Isolation;
import com.programandoenjava.airline.notification.application.transaction.UnitOfWork;
import com.programandoenjava.airline.notification.domain.notification.BookingId;
import com.programandoenjava.airline.notification.domain.notification.Notification;
import com.programandoenjava.airline.notification.domain.notification.NotificationMessage;
import com.programandoenjava.airline.notification.domain.notification.NotificationType;
import com.programandoenjava.airline.notification.domain.notification.PassengerId;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/* A class of its own because @UnitOfWork is proxy-based and does nothing when a
 * bean calls itself. Sending is left outside it on purpose. */
public class NotificationRecorder {

    private final ProcessedEventsPort processedEvents;
    private final SaveNotificationPort saveNotification;

    public NotificationRecorder(final ProcessedEventsPort processedEvents,
                                final SaveNotificationPort saveNotification) {
        this.processedEvents = processedEvents;
        this.saveNotification = saveNotification;
    }

    @UnitOfWork(isolation = Isolation.SERIALIZABLE)
    public Optional<Notification> record(final UUID eventId,
                                         final PassengerId passengerId,
                                         final BookingId bookingId,
                                         final NotificationType type,
                                         final NotificationMessage message,
                                         final Instant now) {

        boolean claimed = processedEvents.claim(eventId);

        if (!claimed) {
            return Optional.empty();
        }

        Notification notification = Notification.raise(passengerId, bookingId, type, message, now);

        saveNotification.save(notification);

        return Optional.of(notification);
    }
}
