package com.programandoenjava.airline.booking.infrastructure.adapter.out.persistence.outbox;

import java.util.List;

interface OutboxLockingQueries {

    List<OutboxEntity> claimPending(int limit);
}
