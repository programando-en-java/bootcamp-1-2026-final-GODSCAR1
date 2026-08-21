package com.programandoenjava.airline.notification.domain.notification;

import com.programandoenjava.airline.notification.domain.shared.DomainValidationException;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

public record Notification(NotificationId id,
                           PassengerId passengerId,
                           BookingId bookingId,
                           NotificationType type,
                           NotificationMessage message,
                           Instant createdAt,
                           @Nullable Instant sentAt) {

    public Notification {
        if (id == null) {
            throw new DomainValidationException("A notification id is required");
        }
        if (passengerId == null) {
            throw new DomainValidationException("A notification must be addressed to a passenger");
        }
        if (bookingId == null) {
            throw new DomainValidationException("A notification must name a booking");
        }
        if (type == null) {
            throw new DomainValidationException("A notification must say what happened");
        }
        if (message == null) {
            throw new DomainValidationException("A notification must carry a message");
        }
        if (createdAt == null) {
            throw new DomainValidationException("A notification must record when it was raised");
        }
    }

    public static Notification raise(final PassengerId passengerId,
                                     final BookingId bookingId,
                                     final NotificationType type,
                                     final NotificationMessage message,
                                     final Instant now) {
        return new Notification(NotificationId.newId(), passengerId, bookingId,
                type, message, now, null);
    }

    /* Callers must use the return value: a record cannot mark itself. */
    public Notification sentAt(final Instant when) {
        if (when == null) {
            throw new DomainValidationException("A send has to have a time");
        }

        return new Notification(id, passengerId, bookingId, type, message, createdAt, when);
    }

    public boolean hasBeenSent() {
        return sentAt != null;
    }
}
