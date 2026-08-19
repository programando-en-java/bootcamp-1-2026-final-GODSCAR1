package com.programandoenjava.airline.checkin.infrastructure.adapter.out.persistence.outbox;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.domain.Limit;

import java.util.List;
import java.util.UUID;

interface OutboxJpaRepository extends JpaRepository<OutboxEntity, UUID> {

    /**
     * The next messages to send, claimed so that nobody else sends them.
     *
     * <p>SKIP LOCKED is required rather than an optimisation: without it two
     * replicas of this service select the same rows and every event goes out
     * twice (ADR-001).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@jakarta.persistence.QueryHint(name = "jakarta.persistence.lock.timeout",
            value = "-2"))
    @Query("SELECT o FROM OutboxEntity o WHERE o.publishedAt IS NULL ORDER BY o.createdAt")
    List<OutboxEntity> claimPending(Limit limit);
}
