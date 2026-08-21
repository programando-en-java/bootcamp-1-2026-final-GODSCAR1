package com.programandoenjava.airline.payment.infrastructure.adapter.out.persistence.outbox;

import java.util.List;

interface OutboxLockingQueries {

    List<OutboxEntity> claimPending(int limit);
}
