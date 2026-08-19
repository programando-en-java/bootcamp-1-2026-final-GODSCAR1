package com.programandoenjava.airline.payment.application.port.out.savepayment;

import com.programandoenjava.airline.payment.domain.payment.Payment;

public interface SavePaymentPort {

    void save(Payment payment);
}
