package com.programandoenjava.airline.payment.infrastructure.adapter.in.web;

import com.programandoenjava.airline.payment.application.port.in.paybooking.PayBookingCommand;
import com.programandoenjava.airline.payment.domain.payment.BookingId;
import com.programandoenjava.airline.payment.domain.payment.CardNumber;
import com.programandoenjava.airline.payment.infrastructure.adapter.in.web.dto.PayBookingRequest;

final class PayBookingRequestMapper {

    private PayBookingRequestMapper() {
    }

    static PayBookingCommand toCommand(final PayBookingRequest request) {
        BookingId booking = new BookingId(request.bookingId());
        CardNumber card = new CardNumber(request.cardNumber());

        return new PayBookingCommand(booking, card);
    }
}
