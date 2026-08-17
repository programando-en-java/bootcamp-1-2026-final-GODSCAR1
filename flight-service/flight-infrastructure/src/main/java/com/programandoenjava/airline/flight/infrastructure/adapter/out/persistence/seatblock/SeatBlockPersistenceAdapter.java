package com.programandoenjava.airline.flight.infrastructure.adapter.out.persistence.seatblock;

import com.programandoenjava.airline.flight.application.port.out.blockseats.FindSeatBlockPort;
import com.programandoenjava.airline.flight.application.port.out.blockseats.SaveSeatBlockPort;
import com.programandoenjava.airline.flight.application.port.shared.IdempotencyKey;
import com.programandoenjava.airline.flight.domain.seatblock.BookingId;
import com.programandoenjava.airline.flight.domain.seatblock.SeatBlock;

import java.util.Optional;

class SeatBlockPersistenceAdapter implements FindSeatBlockPort, SaveSeatBlockPort {

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
    public void save(final SeatBlock block, final IdempotencyKey key) {
        seatBlockJpaRepository.save(SeatBlockEntityMapper.toEntity(block, key));
    }
}
