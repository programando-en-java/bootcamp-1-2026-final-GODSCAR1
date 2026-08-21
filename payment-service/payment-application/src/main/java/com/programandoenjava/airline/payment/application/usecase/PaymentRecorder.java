package com.programandoenjava.airline.payment.application.usecase;

import com.programandoenjava.airline.payment.application.event.PaymentSettled;
import com.programandoenjava.airline.payment.application.port.out.events.DomainEventPublisher;
import com.programandoenjava.airline.payment.application.port.out.savepayment.SavePaymentPort;
import com.programandoenjava.airline.payment.application.transaction.UnitOfWork;
import com.programandoenjava.airline.payment.domain.payment.PassengerId;
import com.programandoenjava.airline.payment.domain.payment.Payment;

/* A class of its own because @UnitOfWork is proxy-based and does nothing when a
 * bean calls itself, and because it keeps the transaction off the gateway call. */
public class PaymentRecorder {

    private final SavePaymentPort savePayment;
    private final DomainEventPublisher events;

    public PaymentRecorder(final SavePaymentPort savePayment,
                           final DomainEventPublisher events) {
        this.savePayment = savePayment;
        this.events = events;
    }

    @UnitOfWork
    public void record(final Payment payment, final PassengerId passengerId) {
        savePayment.save(payment);
        events.publish(new PaymentSettled(payment, passengerId));
    }
}
