package com.programandoenjava.airline.notification.application.port.in.notify;

/**
 * One method per story in EPIC-05. Three rather than one generic notify,
 * because each is composed from a different event and says a different thing,
 * and a single command carrying every field any of them might need would be
 * mostly empty every time.
 */
public interface NotifyPassengerUseCase {

    void onBookingCreated(BookingCreatedCommand command);

    void onPaymentSucceeded(PaymentSucceededCommand command);

    void onCheckInCompleted(CheckInCompletedCommand command);
}
