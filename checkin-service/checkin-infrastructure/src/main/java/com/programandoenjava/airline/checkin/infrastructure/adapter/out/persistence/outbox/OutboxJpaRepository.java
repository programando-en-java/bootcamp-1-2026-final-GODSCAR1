package com.programandoenjava.airline.checkin.infrastructure.adapter.out.persistence.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface OutboxJpaRepository extends JpaRepository<OutboxEntity, UUID>, OutboxLockingQueries {
}
