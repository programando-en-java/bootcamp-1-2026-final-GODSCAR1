package com.programandoenjava.airline.flight.infrastructure.adapter.in.web;

import com.programandoenjava.airline.flight.application.port.in.blockseats.BlockSeatsCommand;
import com.programandoenjava.airline.flight.application.port.in.blockseats.BlockSeatsUseCase;
import com.programandoenjava.airline.flight.application.port.in.blockseats.SeatsHeld;
import com.programandoenjava.airline.flight.application.port.shared.IdempotencyKey;
import com.programandoenjava.airline.flight.application.port.shared.PageQuery;
import com.programandoenjava.airline.flight.application.port.shared.PageResult;
import com.programandoenjava.airline.flight.application.port.in.searchflights.SearchFlightsQuery;
import com.programandoenjava.airline.flight.application.port.in.searchflights.SearchFlightsUseCase;
import com.programandoenjava.airline.flight.domain.flight.Flight;
import com.programandoenjava.airline.flight.domain.flight.FlightId;
import com.programandoenjava.airline.flight.domain.seatblock.BookingId;
import com.programandoenjava.airline.flight.domain.seatblock.SeatCount;
import com.programandoenjava.airline.flight.infrastructure.adapter.in.web.dto.BlockSeatsRequest;
import com.programandoenjava.airline.flight.infrastructure.adapter.in.web.dto.FlightResponse;
import com.programandoenjava.airline.flight.infrastructure.adapter.in.web.dto.SearchFlightsRequest;
import com.programandoenjava.airline.flight.infrastructure.adapter.in.web.dto.SeatBlockResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/flights")
class FlightController {

    private final SearchFlightsUseCase searchFlightsUseCase;

    private final BlockSeatsUseCase blockSeatsUseCase;

    FlightController(final SearchFlightsUseCase searchFlightsUseCase,
                     final BlockSeatsUseCase blockSeatsUseCase) {
        this.searchFlightsUseCase = searchFlightsUseCase;
        this.blockSeatsUseCase = blockSeatsUseCase;
    }


    @GetMapping
    PageResult<FlightResponse> search(
            @Valid final SearchFlightsRequest request,
            @PageableDefault(size = PageQuery.MAX_SIZE) final Pageable pageable) {

        SearchFlightsQuery query = SearchFlightsRequestMapper.toQuery(request, pageable);

        PageResult<Flight> flights = searchFlightsUseCase.search(query);

        return flights.map(FlightResponse::from);
    }

    /**
     * Holds seats on a flight for a booking.
     *
     * <p>The idempotency key is required rather than optional. The only caller is
     * booking-service, and the cost of a forgotten key is seats sold twice; a
     * caller that has to be told to send one is better than a caller that
     * silently double-books on a retry.
     *
     * <p>Answers 201 on both the first request and a repeat of it. A repeat is
     * not an error — the seats it asked for are held, which is what it wanted to
     * know.
     */
    @PostMapping("/{flightId}/seat-blocks")
    @ResponseStatus(HttpStatus.CREATED)
    SeatBlockResponse blockSeats(
            @PathVariable final UUID flightId,
            @RequestHeader("Idempotency-Key") final String idempotencyKey,
            @Valid @RequestBody final BlockSeatsRequest request) {

        BlockSeatsCommand command =
                BlockSeatsRequestMapper.toCommand(flightId, idempotencyKey, request);

        SeatsHeld held = blockSeatsUseCase.block(command);

        return SeatBlockResponse.from(held);
    }
}