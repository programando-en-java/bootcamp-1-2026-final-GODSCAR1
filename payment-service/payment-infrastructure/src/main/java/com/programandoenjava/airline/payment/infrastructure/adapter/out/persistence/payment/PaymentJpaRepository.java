package com.programandoenjava.airline.payment.infrastructure.adapter.out.persistence.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface PaymentJpaRepository extends JpaRepository<PaymentEntity, UUID> {
}
