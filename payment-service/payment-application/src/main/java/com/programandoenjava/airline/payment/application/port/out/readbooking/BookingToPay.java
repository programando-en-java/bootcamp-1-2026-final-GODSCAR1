package com.programandoenjava.airline.payment.application.port.out.readbooking;

import com.programandoenjava.airline.payment.domain.payment.BookingId;
import com.programandoenjava.airline.payment.domain.payment.PassengerId;
import com.programandoenjava.airline.payment.domain.shared.Money;

/**
 * What booking-service says is owed, and whether it is still owed. The status is
 * a plain string: payment-service has no business modelling another service's
 * states, only recognising the one it may charge for.
 */
public record BookingToPay(BookingId bookingId,
                           PassengerId passengerId,
                           Money total,
                           String status) {

    public static final String PAYABLE = "PENDING";

    public BookingToPay {
        if (bookingId == null) {
            throw new IllegalArgumentException("A booking must be named");
        }
        if (passengerId == null) {
            throw new IllegalArgumentException("A passenger must be named");
        }
        if (total == null) {
            throw new IllegalArgumentException("A total is required");
        }
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("A status is required");
        }
    }

    public boolean isPayable() {
        return PAYABLE.equals(status);
    }
}
