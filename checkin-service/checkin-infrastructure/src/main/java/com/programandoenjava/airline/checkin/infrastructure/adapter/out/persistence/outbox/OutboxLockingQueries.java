package com.programandoenjava.airline.checkin.infrastructure.adapter.out.persistence.outbox;

import java.util.List;

interface OutboxLockingQueries {

    List<OutboxEntity> claimPending(int limit);
}
