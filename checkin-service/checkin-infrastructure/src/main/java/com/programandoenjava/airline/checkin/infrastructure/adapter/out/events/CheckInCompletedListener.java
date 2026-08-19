package com.programandoenjava.airline.checkin.infrastructure.adapter.out.events;

import com.programandoenjava.airline.checkin.application.event.CheckInCompleted;
import com.programandoenjava.airline.checkin.domain.boardingpass.BoardingPass;
import com.programandoenjava.airline.checkin.domain.boardingpass.FlightSnapshot;
import com.programandoenjava.airline.checkin.infrastructure.adapter.out.events.dto.CheckInCompletedEvent;
import com.programandoenjava.airline.checkin.infrastructure.adapter.out.persistence.outbox.OutboxWriter;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.util.UUID;

/**
 * Turns the in-process event into the flat one other services read, and puts it
 * in the outbox.
 *
 * <p>BEFORE_COMMIT, so the row joins the transaction that wrote the pass.
 * AFTER_COMMIT would announce a check-in whenever the write that followed
 * failed, and lose one whenever this did (ADR-001).
 *
 * <p>The message is keyed by the booking rather than by the pass, so everything
 * this system says about one journey lands on the same partition and stays in
 * the order it happened.
 */
class CheckInCompletedListener {

    private static final String AGGREGATE_TYPE = "boarding_pass";

    private final OutboxWriter outbox;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    CheckInCompletedListener(final OutboxWriter outbox,
                             final ObjectMapper objectMapper,
                             final Clock clock) {
        this.outbox = outbox;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void on(final CheckInCompleted completed) {
        BoardingPass pass = completed.boardingPass();

        CheckInCompletedEvent event = toEvent(UUID.randomUUID(), pass);

        String payload = objectMapper.writeValueAsString(event);
        String bookingId = pass.bookingId().value().toString();

        outbox.write(AGGREGATE_TYPE, bookingId, CheckInTopics.COMPLETED, payload, clock.instant());
    }

    private static CheckInCompletedEvent toEvent(final UUID eventId, final BoardingPass pass) {
        FlightSnapshot flight = pass.flight();

        return new CheckInCompletedEvent(
                eventId,
                pass.id().value(),
                pass.bookingId().value(),
                pass.passengerId().value(),
                flight.flightId().value(),
                flight.flightNumber(),
                flight.origin(),
                flight.destination(),
                flight.departure(),
                pass.sequence().value(),
                pass.issuedAt());
    }
}
