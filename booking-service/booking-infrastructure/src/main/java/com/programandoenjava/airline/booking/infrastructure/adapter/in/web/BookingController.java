package com.programandoenjava.airline.booking.infrastructure.adapter.in.web;

import com.programandoenjava.airline.booking.application.port.in.createbooking.CreateBookingCommand;
import com.programandoenjava.airline.booking.application.port.in.createbooking.CreateBookingUseCase;
import com.programandoenjava.airline.booking.domain.booking.Booking;
import com.programandoenjava.airline.booking.infrastructure.adapter.in.web.dto.BookingResponse;
import com.programandoenjava.airline.booking.infrastructure.adapter.in.web.dto.CreateBookingRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {

    private final CreateBookingUseCase createBookingUseCase;

    BookingController(final CreateBookingUseCase createBookingUseCase) {
        this.createBookingUseCase = createBookingUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    BookingResponse create(
            @RequestHeader("Idempotency-Key") final String idempotencyKey,
            @Valid @RequestBody final CreateBookingRequest request) {

        CreateBookingCommand command =
                CreateBookingRequestMapper.toCommand(idempotencyKey, request);

        Booking booking = createBookingUseCase.create(command);

        return BookingResponse.from(booking);
    }
}
