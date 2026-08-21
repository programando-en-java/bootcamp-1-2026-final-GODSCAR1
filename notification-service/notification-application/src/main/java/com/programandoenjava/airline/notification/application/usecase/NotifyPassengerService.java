package com.programandoenjava.airline.notification.application.usecase;

import com.programandoenjava.airline.notification.application.port.in.notify.BookingCreatedCommand;
import com.programandoenjava.airline.notification.application.port.in.notify.CheckInCompletedCommand;
import com.programandoenjava.airline.notification.application.port.in.notify.NotifyPassengerUseCase;
import com.programandoenjava.airline.notification.application.port.in.notify.PaymentSucceededCommand;
import com.programandoenjava.airline.notification.application.port.out.channel.NotificationChannel;
import com.programandoenjava.airline.notification.application.port.out.notifications.SaveNotificationPort;
import com.programandoenjava.airline.notification.domain.notification.BookingId;
import com.programandoenjava.airline.notification.domain.notification.Notification;
import com.programandoenjava.airline.notification.domain.notification.NotificationMessage;
import com.programandoenjava.airline.notification.domain.notification.NotificationType;
import com.programandoenjava.airline.notification.domain.notification.PassengerId;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class NotifyPassengerService implements NotifyPassengerUseCase {

    private final NotificationRecorder recorder;
    private final NotificationChannel channel;
    private final SaveNotificationPort saveNotification;
    private final Clock clock;

    public NotifyPassengerService(final NotificationRecorder recorder,
                                  final NotificationChannel channel,
                                  final SaveNotificationPort saveNotification,
                                  final Clock clock) {
        this.recorder = recorder;
        this.channel = channel;
        this.saveNotification = saveNotification;
        this.clock = clock;
    }

    @Override
    public void onBookingCreated(final BookingCreatedCommand command) {
        NotificationMessage message = NotificationMessage.bookingCreated(
                command.seats(), command.total(), command.currency());

        notify(command.eventId(), command.passengerId(), command.bookingId(),
                NotificationType.BOOKING_CREATED, message);
    }

    @Override
    public void onPaymentSucceeded(final PaymentSucceededCommand command) {
        NotificationMessage message = NotificationMessage.paymentSucceeded(
                command.amount(), command.currency());

        notify(command.eventId(), command.passengerId(), command.bookingId(),
                NotificationType.PAYMENT_SUCCEEDED, message);
    }

    @Override
    public void onCheckInCompleted(final CheckInCompletedCommand command) {
        NotificationMessage message = NotificationMessage.checkInCompleted(
                command.flightNumber(), command.origin(), command.destination(),
                command.departureTime(), command.boardingSequence());

        notify(command.eventId(), command.passengerId(), command.bookingId(),
                NotificationType.CHECK_IN_COMPLETED, message);
    }

    /* Written, sent, then marked. A crash in between leaves a notification nobody got,
     * which sent_at makes findable. The other order would send it twice. */
    private void notify(final UUID eventId,
                        final PassengerId passengerId,
                        final BookingId bookingId,
                        final NotificationType type,
                        final NotificationMessage message) {

        Instant now = clock.instant();

        Optional<Notification> raised = recorder.record(
                eventId, passengerId, bookingId, type, message, now);

        if (raised.isEmpty()) {
            return;
        }

        Notification notification = raised.get();

        channel.send(notification);

        Notification sent = notification.sentAt(clock.instant());

        saveNotification.markSent(sent);
    }
}
