package com.programandoenjava.airline.flight.infrastructure.adapter.in.web;

import com.programandoenjava.airline.flight.application.port.in.blockseats.BlockSeatsCommand;
import com.programandoenjava.airline.flight.application.port.in.blockseats.BlockSeatsUseCase;
import com.programandoenjava.airline.flight.application.port.in.blockseats.SeatsHeld;
import com.programandoenjava.airline.flight.application.port.in.readflight.ReadFlightUseCase;
import com.programandoenjava.airline.flight.application.port.in.releaseseats.ReleaseSeatsCommand;
import com.programandoenjava.airline.flight.application.port.in.releaseseats.ReleaseSeatsUseCase;
import com.programandoenjava.airline.flight.application.port.shared.IdempotencyKey;
import com.programandoenjava.airline.flight.application.port.shared.PageQuery;
import com.programandoenjava.airline.flight.application.port.shared.PageResult;
import com.programandoenjava.airline.flight.application.port.in.searchflights.SearchFlightsQuery;
import com.programandoenjava.airline.flight.application.port.in.searchflights.SearchFlightsUseCase;
import com.programandoenjava.airline.flight.domain.flight.Flight;
import com.programandoenjava.airline.flight.domain.flight.FlightId;
import com.programandoenjava.airline.flight.domain.seatblock.BookingId;
import com.programandoenjava.airline.flight.domain.seatblock.SeatBlockId;
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

    private final ReleaseSeatsUseCase releaseSeatsUseCase;

    private final ReadFlightUseCase readFlightUseCase;

    FlightController(final SearchFlightsUseCase searchFlightsUseCase,
                     final BlockSeatsUseCase blockSeatsUseCase,
                     final ReleaseSeatsUseCase releaseSeatsUseCase,
                     final ReadFlightUseCase readFlightUseCase) {
        this.searchFlightsUseCase = searchFlightsUseCase;
        this.blockSeatsUseCase = blockSeatsUseCase;
        this.releaseSeatsUseCase = releaseSeatsUseCase;
        this.readFlightUseCase = readFlightUseCase;
    }

    @GetMapping
    PageResult<FlightResponse> search(
            @Valid final SearchFlightsRequest request,
            @PageableDefault(size = PageQuery.MAX_SIZE) final Pageable pageable) {

        SearchFlightsQuery query = SearchFlightsRequestMapper.toQuery(request, pageable);

        PageResult<Flight> flights = searchFlightsUseCase.search(query);

        return flights.map(FlightResponse::from);
    }

    @GetMapping("/{flightId}")
    FlightResponse byId(@PathVariable final UUID flightId) {
        FlightId id = new FlightId(flightId);

        Flight flight = readFlightUseCase.byId(id);

        return FlightResponse.from(flight);
    }

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

    @DeleteMapping("/{flightId}/seat-blocks/{seatBlockId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void releaseSeats(@PathVariable final UUID flightId,
                      @PathVariable final UUID seatBlockId) {

        FlightId flight = new FlightId(flightId);
        SeatBlockId seatBlock = new SeatBlockId(seatBlockId);
        ReleaseSeatsCommand command = new ReleaseSeatsCommand(flight, seatBlock);

        releaseSeatsUseCase.release(command);
    }
}
