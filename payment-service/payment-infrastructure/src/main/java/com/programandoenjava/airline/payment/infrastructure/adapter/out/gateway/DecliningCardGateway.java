package com.programandoenjava.airline.payment.infrastructure.adapter.out.gateway;

import com.programandoenjava.airline.payment.application.port.out.gateway.ChargeRequest;
import com.programandoenjava.airline.payment.application.port.out.gateway.PaymentGatewayPort;

class DecliningCardGateway implements PaymentGatewayPort {

    private static final String DECLINED = "4000000000000002";

    @Override
    public boolean charge(final ChargeRequest request) {
        String card = request.card().value();

        return !DECLINED.equals(card);
    }
}
