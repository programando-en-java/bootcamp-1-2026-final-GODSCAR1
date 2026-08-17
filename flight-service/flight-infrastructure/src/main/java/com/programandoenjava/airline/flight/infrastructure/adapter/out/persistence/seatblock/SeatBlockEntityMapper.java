package com.programandoenjava.airline.flight.infrastructure.adapter.out.persistence.seatblock;

import com.programandoenjava.airline.flight.application.port.shared.IdempotencyKey;
import com.programandoenjava.airline.flight.domain.flight.FlightId;
import com.programandoenjava.airline.flight.domain.seatblock.BookingId;
import com.programandoenjava.airline.flight.domain.seatblock.SeatBlock;
import com.programandoenjava.airline.flight.domain.seatblock.SeatBlockId;
import com.programandoenjava.airline.flight.domain.seatblock.SeatCount;

final class SeatBlockEntityMapper {

    private SeatBlockEntityMapper() {
    }

    static SeatBlockEntity toEntity(final SeatBlock block, final IdempotencyKey key) {
        return new SeatBlockEntity(
                block.id().value(),
                block.flightId().value(),
                block.bookingId().value(),
                block.seats().value(),
                key.value(),
                block.blockedAt());
    }

    static SeatBlock toDomain(final SeatBlockEntity entity) {
        return new SeatBlock(
                new SeatBlockId(entity.getId()),
                new FlightId(entity.getFlightId()),
                new BookingId(entity.getBookingId()),
                new SeatCount(entity.getSeats()),
                entity.getBlockedAt());
    }
}