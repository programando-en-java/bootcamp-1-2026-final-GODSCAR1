package com.programandoenjava.airline.notification.infrastructure.adapter.out.persistence.notifications;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface NotificationJpaRepository extends JpaRepository<NotificationEntity, UUID> {
}
