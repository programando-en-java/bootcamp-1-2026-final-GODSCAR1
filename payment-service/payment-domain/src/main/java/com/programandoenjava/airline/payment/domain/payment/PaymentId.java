package com.programandoenjava.airline.payment.domain.payment;

import com.programandoenjava.airline.payment.domain.shared.DomainValidationException;

import java.util.UUID;

public record PaymentId(UUID value) {

    public PaymentId {
        if (value == null) {
            throw new DomainValidationException("A payment id is required");
        }
    }

    public static PaymentId newId() {
        UUID generated = UUID.randomUUID();

        return new PaymentId(generated);
    }
}
