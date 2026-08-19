package com.programandoenjava.airline.payment.application.usecase;

import com.programandoenjava.airline.payment.application.event.PaymentSettled;
import com.programandoenjava.airline.payment.application.port.out.events.DomainEventPublisher;
import com.programandoenjava.airline.payment.application.port.out.savepayment.SavePaymentPort;
import com.programandoenjava.airline.payment.application.transaction.UnitOfWork;
import com.programandoenjava.airline.payment.domain.payment.Payment;

/**
 * The two writes that have to happen together: the payment, and the outbox row
 * the listener adds when the event is published (ADR-001). A crash between them
 * would leave a charge nobody announced, or an announcement of a charge that
 * never landed.
 *
 * <p>A class of its own rather than a method on PayBookingService, because
 * {@code @UnitOfWork} is proxy-based and does nothing when a bean calls itself.
 * Keeping it here also keeps the transaction off the two network calls that
 * precede it.
 */
public class PaymentRecorder {

    private final SavePaymentPort savePayment;
    private final DomainEventPublisher events;

    public PaymentRecorder(final SavePaymentPort savePayment,
                           final DomainEventPublisher events) {
        this.savePayment = savePayment;
        this.events = events;
    }

    @UnitOfWork
    public void record(final Payment payment) {
        savePayment.save(payment);
        events.publish(new PaymentSettled(payment));
    }
}
