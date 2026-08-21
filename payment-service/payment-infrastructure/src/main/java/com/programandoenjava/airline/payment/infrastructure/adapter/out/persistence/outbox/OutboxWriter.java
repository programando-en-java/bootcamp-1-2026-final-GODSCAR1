package com.programandoenjava.airline.payment.infrastructure.adapter.out.persistence.outbox;

import java.time.Instant;
import java.util.UUID;

public interface OutboxWriter {

    UUID write(String aggregateType, String aggregateId, String topic, String payload,
               Instant now);
}
