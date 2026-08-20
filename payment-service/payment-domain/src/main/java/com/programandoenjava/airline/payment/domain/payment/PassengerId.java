package com.programandoenjava.airline.payment.domain.payment;

import com.programandoenjava.airline.payment.domain.shared.DomainValidationException;

import java.util.UUID;

/**
 * Who the booking belongs to. Payment never reasons about it and Payment does
 * not hold it: it is read with the booking and carried into the announcement,
 * because an announcement is addressed to someone.
 */
public record PassengerId(UUID value) {

    public PassengerId {
        if (value == null) {
            throw new DomainValidationException("A passenger id is required");
        }
    }
}
