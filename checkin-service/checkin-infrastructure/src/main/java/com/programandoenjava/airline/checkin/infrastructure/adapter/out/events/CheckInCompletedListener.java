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

    /* BEFORE_COMMIT, so the row joins the transaction that caused it. AFTER_COMMIT
     * would announce what never happened and lose what did (ADR-001). */
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
