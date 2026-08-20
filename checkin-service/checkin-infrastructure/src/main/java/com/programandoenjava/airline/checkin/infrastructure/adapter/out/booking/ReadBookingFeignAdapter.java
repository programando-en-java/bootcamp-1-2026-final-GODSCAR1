package com.programandoenjava.airline.checkin.infrastructure.adapter.out.booking;

import com.programandoenjava.airline.checkin.application.port.out.readbooking.BookingToCheckIn;
import com.programandoenjava.airline.checkin.application.port.out.readbooking.ReadBookingPort;
import com.programandoenjava.airline.checkin.application.port.out.readbooking.exception.BookingNotFoundException;
import com.programandoenjava.airline.checkin.domain.boardingpass.BookingId;
import com.programandoenjava.airline.checkin.domain.boardingpass.FlightId;
import com.programandoenjava.airline.checkin.domain.boardingpass.PassengerId;
import com.programandoenjava.airline.checkin.infrastructure.adapter.out.booking.dto.BookingResponse;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.http.HttpStatus;

class ReadBookingFeignAdapter implements ReadBookingPort {

    private static final String BOOKING_SERVICE = "bookingService";

    private static final int NOT_FOUND = HttpStatus.NOT_FOUND.value();

    private final BookingClient bookingClient;

    ReadBookingFeignAdapter(final BookingClient bookingClient) {
        this.bookingClient = bookingClient;
    }

    @Override
    @Retry(name = BOOKING_SERVICE)
    @CircuitBreaker(name = BOOKING_SERVICE)
    public BookingToCheckIn byId(final BookingId bookingId) {
        BookingResponse response = read(bookingId);

        PassengerId passenger = new PassengerId(response.passengerId());
        FlightId flight = new FlightId(response.flightId());

        return new BookingToCheckIn(bookingId, passenger, flight, response.status());
    }

    /*
     * The 404 becomes an exception naming the booking here, where the question
     * is known. Compared by number rather than by a Spring constant, which is
     * the lesson ADR-012 records.
     */
    private BookingResponse read(final BookingId bookingId) {
        try {
            return bookingClient.byId(bookingId.value());
        } catch (FeignException failed) {
            if (failed.status() == NOT_FOUND) {
                throw new BookingNotFoundException(bookingId);
            }
            throw failed;
        }
    }
}
