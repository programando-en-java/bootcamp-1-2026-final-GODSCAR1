package com.programandoenjava.airline.booking.infrastructure.adapter.out.persistence.booking;

import com.programandoenjava.airline.booking.application.port.shared.IdempotencyKey;
import com.programandoenjava.airline.booking.domain.booking.Booking;
import com.programandoenjava.airline.booking.domain.booking.BookingId;
import com.programandoenjava.airline.booking.domain.booking.FlightId;
import com.programandoenjava.airline.booking.domain.booking.PassengerId;
import com.programandoenjava.airline.booking.domain.booking.SeatBlockId;
import com.programandoenjava.airline.booking.domain.booking.SeatCount;
import com.programandoenjava.airline.booking.domain.shared.Money;

import java.util.Currency;

final class BookingEntityMapper {

    private BookingEntityMapper() {
    }

    static BookingEntity toEntity(final Booking booking, final IdempotencyKey key) {
        String priceCurrency = booking.pricePerSeat().currency().getCurrencyCode();
        String totalCurrency = booking.total().currency().getCurrencyCode();

        return new BookingEntity(
                booking.id().value(),
                booking.passengerId().value(),
                booking.flightId().value(),
                booking.seatBlockId().value(),
                booking.seats().value(),
                booking.pricePerSeat().amount(),
                priceCurrency,
                booking.total().amount(),
                totalCurrency,
                booking.status(),
                key.value(),
                booking.createdAt());
    }

    static Booking toDomain(final BookingEntity entity) {
        Currency priceCurrency = Currency.getInstance(entity.getPriceCurrency());
        Currency totalCurrency = Currency.getInstance(entity.getTotalCurrency());

        Money pricePerSeat = new Money(entity.getPriceAmount(), priceCurrency);
        Money total = new Money(entity.getTotalAmount(), totalCurrency);

        BookingId id = new BookingId(entity.getId());
        PassengerId passengerId = new PassengerId(entity.getPassengerId());
        FlightId flightId = new FlightId(entity.getFlightId());
        SeatBlockId seatBlockId = new SeatBlockId(entity.getSeatBlockId());
        SeatCount seats = new SeatCount(entity.getSeats());

        return new Booking(id, passengerId, flightId, seatBlockId, seats,
                pricePerSeat, total, entity.getStatus(), entity.getCreatedAt());
    }
}
