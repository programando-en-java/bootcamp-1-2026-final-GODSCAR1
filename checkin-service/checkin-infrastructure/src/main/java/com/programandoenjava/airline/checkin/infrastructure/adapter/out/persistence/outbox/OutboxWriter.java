package com.programandoenjava.airline.checkin.infrastructure.adapter.out.persistence.outbox;

import java.time.Instant;
import java.util.UUID;

/**
 * Adds a message to the outbox. Called from a listener running before the
 * business transaction commits, so the row lands with the state change or not
 * at all (ADR-001).
 */
public interface OutboxWriter {

    UUID write(String aggregateType, String aggregateId, String topic, String payload,
               Instant now);
}
