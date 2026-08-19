package com.programandoenjava.airline.payment.application.port.in.paybooking;

import com.programandoenjava.airline.payment.domain.payment.Payment;

/**
 * Charges for a booking. A refusal comes back as a Payment that failed, not as
 * an exception: the charge was attempted and that is what happened.
 */
public interface PayBookingUseCase {

    Payment pay(PayBookingCommand command);
}
