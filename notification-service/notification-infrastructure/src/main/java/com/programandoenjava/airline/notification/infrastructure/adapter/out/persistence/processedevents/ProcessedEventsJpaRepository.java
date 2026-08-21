package com.programandoenjava.airline.notification.infrastructure.adapter.out.persistence.processedevents;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface ProcessedEventsJpaRepository extends JpaRepository<ProcessedEventEntity, UUID> {
}
