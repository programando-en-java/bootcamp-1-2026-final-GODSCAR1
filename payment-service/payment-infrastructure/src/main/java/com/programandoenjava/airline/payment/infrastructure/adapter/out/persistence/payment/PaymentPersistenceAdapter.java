package com.programandoenjava.airline.payment.infrastructure.adapter.out.persistence.payment;

import com.programandoenjava.airline.payment.application.port.out.savepayment.SavePaymentPort;
import com.programandoenjava.airline.payment.domain.payment.Payment;
import org.springframework.transaction.annotation.Transactional;

class PaymentPersistenceAdapter implements SavePaymentPort {

    private final PaymentJpaRepository paymentJpaRepository;

    PaymentPersistenceAdapter(final PaymentJpaRepository paymentJpaRepository) {
        this.paymentJpaRepository = paymentJpaRepository;
    }

    @Override
    @Transactional
    public void save(final Payment payment) {
        paymentJpaRepository.save(PaymentEntityMapper.toEntity(payment));
    }
}
