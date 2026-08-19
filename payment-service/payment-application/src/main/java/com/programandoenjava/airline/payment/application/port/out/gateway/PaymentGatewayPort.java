package com.programandoenjava.airline.payment.application.port.out.gateway;

/**
 * Whoever moves the money. A decline is a false answer, not an exception: the
 * gateway did its job and the answer was no.
 */
public interface PaymentGatewayPort {

    boolean charge(ChargeRequest request);
}
