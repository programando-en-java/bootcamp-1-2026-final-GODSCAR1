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

    /* BEFORE_COMMIT, so the row joins the transaction that caused it. AFTER_COMMIT
     * would announce what never happened and lose what did (ADR-001). */
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
