package com.programandoenjava.airline.notification.application.port.in.notify;

public interface NotifyPassengerUseCase {

    void onBookingCreated(BookingCreatedCommand command);

    void onPaymentSucceeded(PaymentSucceededCommand command);

    void onCheckInCompleted(CheckInCompletedCommand command);
}
