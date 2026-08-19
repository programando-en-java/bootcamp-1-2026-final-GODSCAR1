package com.programandoenjava.airline.payment.infrastructure.adapter.out.gateway;

import com.programandoenjava.airline.payment.application.port.out.gateway.ChargeRequest;
import com.programandoenjava.airline.payment.application.port.out.gateway.PaymentGatewayPort;

/**
 * A stand-in for an acquirer, deciding by card number rather than by chance
 * (ADR-013). A random adapter would make US-006 untestable: an end-to-end test
 * that fails one run in ten teaches people to re-run rather than to read.
 *
 * <p>The declining number is the one Stripe documents for the same purpose, so
 * anyone who has seen a payment integration recognises it.
 */
class DecliningCardGateway implements PaymentGatewayPort {

    private static final String DECLINED = "4000000000000002";

    @Override
    public boolean charge(final ChargeRequest request) {
        String card = request.card().value();

        return !DECLINED.equals(card);
    }
}
