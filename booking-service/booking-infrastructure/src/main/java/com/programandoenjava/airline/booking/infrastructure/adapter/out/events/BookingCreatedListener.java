package com.programandoenjava.airline.booking.infrastructure.adapter.out.events;

import com.programandoenjava.airline.booking.application.event.BookingCreated;
import com.programandoenjava.airline.booking.domain.booking.Booking;
import com.programandoenjava.airline.booking.infrastructure.adapter.out.events.dto.BookingCreatedEvent;
import com.programandoenjava.airline.booking.infrastructure.adapter.out.persistence.outbox.OutboxWriter;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.util.UUID;

/**
 * Turns the in-process event into the flat one other services read, and puts it
 * in the outbox.
 *
 * <p>BEFORE_COMMIT, so the row joins the transaction that wrote the booking.
 * AFTER_COMMIT would announce a booking whenever the write that followed
 * failed, and lose one whenever this did (ADR-001).
 */
class BookingCreatedListener {

    private static final String AGGREGATE_TYPE = "booking";

    private final OutboxWriter outbox;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    BookingCreatedListener(final OutboxWriter outbox,
                           final ObjectMapper objectMapper,
                           final Clock clock) {
        this.outbox = outbox;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void on(final BookingCreated created) {
        Booking booking = created.booking();

        BookingCreatedEvent event = toEvent(UUID.randomUUID(), booking);

        String payload = objectMapper.writeValueAsString(event);
        String bookingId = booking.id().value().toString();

        outbox.write(AGGREGATE_TYPE, bookingId, BookingTopics.CREATED, payload, clock.instant());
    }

    private static BookingCreatedEvent toEvent(final UUID eventId, final Booking booking) {
        return new BookingCreatedEvent(
                eventId,
                booking.id().value(),
                booking.passengerId().value(),
                booking.flightId().value(),
                booking.seats().value(),
                booking.total().amount(),
                booking.total().currency().getCurrencyCode(),
                booking.status().name(),
                booking.createdAt());
    }
}
