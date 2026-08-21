package com.programandoenjava.airline.payment.infrastructure.adapter.out.booking;

import com.programandoenjava.airline.payment.application.port.out.readbooking.BookingToPay;
import com.programandoenjava.airline.payment.application.port.out.readbooking.ReadBookingPort;
import com.programandoenjava.airline.payment.application.port.out.readbooking.exception.BookingNotFoundException;
import com.programandoenjava.airline.payment.domain.payment.BookingId;
import com.programandoenjava.airline.payment.domain.payment.PassengerId;
import com.programandoenjava.airline.payment.domain.shared.Money;
import com.programandoenjava.airline.payment.infrastructure.adapter.out.booking.dto.BookingResponse;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

import java.util.Currency;

class ReadBookingFeignAdapter implements ReadBookingPort {

    private static final String BOOKING_SERVICE = "bookingService";

    private final BookingClient bookingClient;

    ReadBookingFeignAdapter(final BookingClient bookingClient) {
        this.bookingClient = bookingClient;
    }

    @Override
    @Retry(name = BOOKING_SERVICE)
    @CircuitBreaker(name = BOOKING_SERVICE)
    public BookingToPay byId(final BookingId bookingId) {
        BookingResponse response = read(bookingId);

        Currency currency = Currency.getInstance(response.currency());
        Money total = new Money(response.total(), currency);
        PassengerId passenger = new PassengerId(response.passengerId());

        return new BookingToPay(bookingId, passenger, total, response.status());
    }

    private BookingResponse read(final BookingId bookingId) {
        try {
            return bookingClient.byId(bookingId.value());
        } catch (FeignException failed) {
            if (failed.status() == BookingErrorDecoder.NOT_FOUND) {
                throw new BookingNotFoundException(bookingId);
            }
            throw failed;
        }
    }
}
