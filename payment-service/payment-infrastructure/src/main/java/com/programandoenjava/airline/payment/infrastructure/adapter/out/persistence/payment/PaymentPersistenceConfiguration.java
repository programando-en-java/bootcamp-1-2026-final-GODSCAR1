package com.programandoenjava.airline.payment.infrastructure.adapter.out.persistence.payment;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentPersistenceConfiguration {

    @Bean
    PaymentPersistenceAdapter paymentPersistenceAdapter(
            final PaymentJpaRepository paymentJpaRepository) {
        return new PaymentPersistenceAdapter(paymentJpaRepository);
    }
}
