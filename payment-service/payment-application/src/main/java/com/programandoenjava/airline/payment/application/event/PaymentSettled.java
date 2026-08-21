package com.programandoenjava.airline.payment.application.event;

import com.programandoenjava.airline.payment.domain.payment.PassengerId;
import com.programandoenjava.airline.payment.domain.payment.Payment;

public record PaymentSettled(Payment payment, PassengerId passengerId) {

    public PaymentSettled {
        if (payment == null) {
            throw new IllegalArgumentException("A settled payment is required");
        }
        if (passengerId == null) {
            throw new IllegalArgumentException("A passenger is required");
        }
    }
}
