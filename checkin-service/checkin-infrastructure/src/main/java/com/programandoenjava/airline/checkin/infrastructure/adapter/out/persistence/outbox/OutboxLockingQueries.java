package com.programandoenjava.airline.checkin.infrastructure.adapter.out.persistence.outbox;

import java.util.List;

/**
 * The one query the relay needs that Spring Data cannot derive, kept out of the
 * repository interface so it can be written with the criteria API instead of a
 * string.
 */
interface OutboxLockingQueries {

    /**
     * The next messages to send, claimed so that nobody else sends them.
     *
     * <p>SKIP LOCKED is required rather than an optimisation: without it two
     * replicas of this service select the same rows and every event goes out
     * twice (ADR-001).
     */
    List<OutboxEntity> claimPending(int limit);
}
