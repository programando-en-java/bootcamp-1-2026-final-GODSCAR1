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

/**
 * Claims the event and writes the notification, in one transaction. A class of
 * its own rather than a method on NotifyPassengerService, because
 * {@code @UnitOfWork} is proxy-based and does nothing when a bean calls itself.
 *
 * <p>The claim and the write share a transaction so that neither can happen
 * without the other. Sending is deliberately outside: a channel is somebody
 * else's system, and holding a database transaction open across it is the
 * mistake this codebase has avoided everywhere else.
 */
public class NotificationRecorder {

    private final ProcessedEventsPort processedEvents;
    private final SaveNotificationPort saveNotification;

    public NotificationRecorder(final ProcessedEventsPort processedEvents,
                                final SaveNotificationPort saveNotification) {
        this.processedEvents = processedEvents;
        this.saveNotification = saveNotification;
    }

    /** Empty when this event has already been acted on, and nothing was written. */
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
