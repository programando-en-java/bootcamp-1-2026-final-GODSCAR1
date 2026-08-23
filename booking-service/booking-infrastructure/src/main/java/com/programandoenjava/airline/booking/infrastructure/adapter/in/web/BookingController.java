package com.programandoenjava.airline.booking.infrastructure.adapter.in.web;

import com.programandoenjava.airline.booking.application.port.in.createbooking.CreateBookingCommand;
import com.programandoenjava.airline.booking.application.port.in.createbooking.CreateBookingUseCase;
import com.programandoenjava.airline.booking.application.port.in.readbooking.ReadBookingUseCase;
import com.programandoenjava.airline.booking.application.port.shared.Caller;
import com.programandoenjava.airline.booking.domain.booking.BookingId;
import com.programandoenjava.airline.booking.domain.booking.Booking;
import com.programandoenjava.airline.booking.infrastructure.adapter.in.web.dto.BookingResponse;
import com.programandoenjava.airline.booking.infrastructure.adapter.in.web.dto.CreateBookingRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {

    private final CreateBookingUseCase createBookingUseCase;

    private final ReadBookingUseCase readBookingUseCase;

    BookingController(final CreateBookingUseCase createBookingUseCase,
                      final ReadBookingUseCase readBookingUseCase) {
        this.createBookingUseCase = createBookingUseCase;
        this.readBookingUseCase = readBookingUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    BookingResponse create(
            @AuthenticationPrincipal final Jwt token,
            @RequestHeader("Idempotency-Key") final String idempotencyKey,
            @Valid @RequestBody final CreateBookingRequest request) {

        UUID passengerId = UUID.fromString(token.getSubject());

        CreateBookingCommand command =
                CreateBookingRequestMapper.toCommand(passengerId, idempotencyKey, request);

        Booking booking = createBookingUseCase.create(command);

        return BookingResponse.from(booking);
    }

    @GetMapping("/{bookingId}")
    BookingResponse byId(@AuthenticationPrincipal final Jwt token,
                         @PathVariable final UUID bookingId) {
        BookingId id = new BookingId(bookingId);
        Caller caller = CallerFromToken.of(token);

        Booking booking = readBookingUseCase.byId(id, caller);

        return BookingResponse.from(booking);
    }
}
