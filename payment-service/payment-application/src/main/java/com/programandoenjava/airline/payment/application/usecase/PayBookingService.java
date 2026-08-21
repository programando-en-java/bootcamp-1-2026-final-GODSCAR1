package com.programandoenjava.airline.payment.application.usecase;

import com.programandoenjava.airline.payment.application.port.in.paybooking.PayBookingCommand;
import com.programandoenjava.airline.payment.application.port.in.paybooking.PayBookingUseCase;
import com.programandoenjava.airline.payment.application.port.in.paybooking.exception.BookingNotPayableException;
import com.programandoenjava.airline.payment.application.port.out.gateway.ChargeRequest;
import com.programandoenjava.airline.payment.application.port.out.gateway.PaymentGatewayPort;
import com.programandoenjava.airline.payment.application.port.out.readbooking.BookingToPay;
import com.programandoenjava.airline.payment.application.port.out.readbooking.ReadBookingPort;
import com.programandoenjava.airline.payment.domain.payment.Payment;
import com.programandoenjava.airline.payment.domain.payment.PaymentId;
import com.programandoenjava.airline.payment.domain.shared.Money;

import java.time.Clock;
import java.time.Instant;

public class PayBookingService implements PayBookingUseCase {

    private final ReadBookingPort readBooking;
    private final PaymentGatewayPort gateway;
    private final PaymentRecorder recorder;
    private final Clock clock;

    public PayBookingService(final ReadBookingPort readBooking,
                             final PaymentGatewayPort gateway,
                             final PaymentRecorder recorder,
                             final Clock clock) {
        this.readBooking = readBooking;
        this.gateway = gateway;
        this.recorder = recorder;
        this.clock = clock;
    }

    @Override
    public Payment pay(final PayBookingCommand command) {
        BookingToPay booking = readBooking.byId(command.bookingId());
        if (!booking.isPayable()) {
            throw new BookingNotPayableException(command.bookingId(), booking.status());
        }

        Money amount = booking.total();
        ChargeRequest charge = new ChargeRequest(amount, command.card());
        boolean charged = gateway.charge(charge);

        PaymentId id = PaymentId.newId();
        Instant now = clock.instant();

        Payment payment = charged
                ? Payment.succeeded(id, command.bookingId(), amount, command.card(), now)
                : Payment.failed(id, command.bookingId(), amount, command.card(), now);

        recorder.record(payment, booking.passengerId());

        return payment;
    }
}
