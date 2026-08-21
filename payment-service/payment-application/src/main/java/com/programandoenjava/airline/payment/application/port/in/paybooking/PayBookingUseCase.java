package com.programandoenjava.airline.payment.application.port.in.paybooking;

import com.programandoenjava.airline.payment.domain.payment.Payment;

public interface PayBookingUseCase {

    Payment pay(PayBookingCommand command);
}
