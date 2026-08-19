package com.programandoenjava.airline.flight.infrastructure.adapter.out.persistence.seatblock;

import com.programandoenjava.airline.flight.application.port.out.seatblock.DeleteSeatBlockPort;
import com.programandoenjava.airline.flight.application.port.out.seatblock.FindSeatBlockPort;
import com.programandoenjava.airline.flight.application.port.out.seatblock.SaveSeatBlockPort;
import com.programandoenjava.airline.flight.application.port.shared.IdempotencyKey;
import com.programandoenjava.airline.flight.domain.flight.FlightId;
import com.programandoenjava.airline.flight.domain.seatblock.BookingId;
import com.programandoenjava.airline.flight.domain.seatblock.SeatBlock;
import com.programandoenjava.airline.flight.domain.seatblock.SeatBlockId;

import java.util.Optional;

class SeatBlockPersistenceAdapter
        implements FindSeatBlockPort, SaveSeatBlockPort, DeleteSeatBlockPort {

    private final SeatBlockJpaRepository seatBlockJpaRepository;

    SeatBlockPersistenceAdapter(final SeatBlockJpaRepository seatBlockJpaRepository) {
        this.seatBlockJpaRepository = seatBlockJpaRepository;
    }

    @Override
    public Optional<SeatBlock> byIdempotencyKey(final IdempotencyKey key) {
        return seatBlockJpaRepository.findByIdempotencyKey(key.value())
                .map(SeatBlockEntityMapper::toDomain);
    }

    @Override
    public boolean existsForBooking(final BookingId bookingId) {
        return seatBlockJpaRepository.existsByBookingId(bookingId.value());
    }

    @Override
    public Optional<SeatBlock> byIdOnFlight(final SeatBlockId id, final FlightId flightId) {
        return seatBlockJpaRepository.findByIdAndFlightId(id.value(), flightId.value())
                .map(SeatBlockEntityMapper::toDomain);
    }

    @Override
    public void save(final SeatBlock block, final IdempotencyKey key) {
        seatBlockJpaRepository.save(SeatBlockEntityMapper.toEntity(block, key));
    }

    @Override
    public void delete(final SeatBlockId id) {
        seatBlockJpaRepository.deleteById(id.value());
    }
}
