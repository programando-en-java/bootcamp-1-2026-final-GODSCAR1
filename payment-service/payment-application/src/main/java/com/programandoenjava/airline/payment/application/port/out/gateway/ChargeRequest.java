package com.programandoenjava.airline.payment.application.port.out.gateway;

import com.programandoenjava.airline.payment.domain.payment.CardNumber;
import com.programandoenjava.airline.payment.domain.shared.Money;

public record ChargeRequest(Money amount, CardNumber card) {

    public ChargeRequest {
        if (amount == null) {
            throw new IllegalArgumentException("An amount is required");
        }
        if (card == null) {
            throw new IllegalArgumentException("A card is required");
        }
    }
}
