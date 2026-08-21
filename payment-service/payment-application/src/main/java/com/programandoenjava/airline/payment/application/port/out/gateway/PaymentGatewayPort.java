package com.programandoenjava.airline.payment.application.port.out.gateway;

public interface PaymentGatewayPort {

    boolean charge(ChargeRequest request);
}
