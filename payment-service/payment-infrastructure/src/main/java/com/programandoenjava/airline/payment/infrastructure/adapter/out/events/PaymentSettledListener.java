package com.programandoenjava.airline.payment.infrastructure.adapter.out.events;

import com.programandoenjava.airline.payment.application.event.PaymentSettled;
import com.programandoenjava.airline.payment.domain.payment.Payment;
import com.programandoenjava.airline.payment.infrastructure.adapter.out.events.dto.PaymentFailedEvent;
import com.programandoenjava.airline.payment.infrastructure.adapter.out.events.dto.PaymentSucceededEvent;
import com.programandoenjava.airline.payment.infrastructure.adapter.out.persistence.outbox.OutboxWriter;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.util.UUID;

class PaymentSettledListener {

    private static final String AGGREGATE_TYPE = "payment";

    private final OutboxWriter outbox;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    PaymentSettledListener(final OutboxWriter outbox,
                           final ObjectMapper objectMapper,
                           final Clock clock) {
        this.outbox = outbox;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    /* BEFORE_COMMIT, so the row joins the transaction that caused it. AFTER_COMMIT
     * would announce what never happened and lose what did (ADR-001). */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void on(final PaymentSettled settled) {
        Payment payment = settled.payment();
        UUID passengerId = settled.passengerId().value();
        UUID eventId = UUID.randomUUID();

        String topic = payment.hasSucceeded() ? PaymentTopics.SUCCEEDED : PaymentTopics.FAILED;
        Object event = payment.hasSucceeded()
                ? succeeded(eventId, payment, passengerId)
                : failed(eventId, payment, passengerId);

        String payload = objectMapper.writeValueAsString(event);
        String bookingId = payment.bookingId().value().toString();

        outbox.write(AGGREGATE_TYPE, bookingId, topic, payload, clock.instant());
    }

    private static PaymentSucceededEvent succeeded(final UUID eventId,
                                                   final Payment payment,
                                                   final UUID passengerId) {
        return new PaymentSucceededEvent(
                eventId,
                payment.id().value(),
                payment.bookingId().value(),
                passengerId,
                payment.amount().amount(),
                payment.amount().currency().getCurrencyCode(),
                payment.processedAt());
    }

    private static PaymentFailedEvent failed(final UUID eventId,
                                             final Payment payment,
                                             final UUID passengerId) {
        return new PaymentFailedEvent(
                eventId,
                payment.id().value(),
                payment.bookingId().value(),
                passengerId,
                payment.amount().amount(),
                payment.amount().currency().getCurrencyCode(),
                payment.processedAt());
    }
}
