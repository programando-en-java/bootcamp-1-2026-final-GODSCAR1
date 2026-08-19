package com.programandoenjava.airline.booking.infrastructure.adapter.out.persistence.booking;

import com.programandoenjava.airline.booking.application.port.in.readbooking.exception.BookingNotFoundException;
import com.programandoenjava.airline.booking.application.port.out.findbooking.FindBookingPort;
import com.programandoenjava.airline.booking.application.port.out.savebooking.SaveBookingPort;
import com.programandoenjava.airline.booking.application.port.shared.IdempotencyKey;
import com.programandoenjava.airline.booking.domain.booking.Booking;
import com.programandoenjava.airline.booking.domain.booking.BookingId;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

class BookingPersistenceAdapter implements FindBookingPort, SaveBookingPort {

    private final BookingJpaRepository bookingJpaRepository;

    BookingPersistenceAdapter(final BookingJpaRepository bookingJpaRepository) {
        this.bookingJpaRepository = bookingJpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Booking> byIdempotencyKey(final IdempotencyKey key) {
        return findByKey(key);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Booking> byId(final BookingId bookingId) {
        return bookingJpaRepository.findById(bookingId.value())
                .map(BookingEntityMapper::toDomain);
    }

    @Override
    @Transactional
    public Optional<Booking> saveIfNew(final Booking booking, final IdempotencyKey key) {
        int inserted = insert(booking, key);

        if (inserted == 1) {
            return Optional.empty();
        }
        return findByKey(key);
    }

    /*
     * Loads and mutates rather than rewriting the row, so the idempotency key
     * and everything else the booking does not carry stay as they were.
     */
    @Override
    @Transactional
    public void updateStatus(final Booking booking) {
        BookingEntity entity = load(booking.id());

        entity.settleAs(booking.status());
    }

    @Override
    @Transactional
    public void markSeatsReleased(final Booking booking, final Instant at) {
        BookingEntity entity = load(booking.id());

        entity.recordSeatsReleased(at);
    }

    private BookingEntity load(final BookingId bookingId) {
        return bookingJpaRepository.findById(bookingId.value())
                .orElseThrow(() -> new BookingNotFoundException(bookingId));
    }

    private Optional<Booking> findByKey(final IdempotencyKey key) {
        return bookingJpaRepository.findByIdempotencyKey(key.value())
                .map(BookingEntityMapper::toDomain);
    }

    private int insert(final Booking booking, final IdempotencyKey key) {
        String status = booking.status().name();
        String priceCurrency = booking.pricePerSeat().currency().getCurrencyCode();
        String totalCurrency = booking.total().currency().getCurrencyCode();

        return bookingJpaRepository.insertIfAbsent(
                booking.id().value(),
                booking.passengerId().value(),
                booking.flightId().value(),
                booking.seatBlockId().value(),
                booking.seats().value(),
                booking.pricePerSeat().amount(),
                priceCurrency,
                booking.total().amount(),
                totalCurrency,
                status,
                key.value(),
                booking.createdAt());
    }
}
