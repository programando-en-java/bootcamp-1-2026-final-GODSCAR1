package com.programandoenjava.airline.payment.infrastructure.transaction;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * EnableAspectJAutoProxy is explicit because a hand-built slice runs with
 * auto-configuration off, and without proxying the aspect never fires while the
 * tests still pass.
 */
@Configuration(proxyBeanMethods = false)
@EnableAspectJAutoProxy
public class TransactionSupportConfiguration {

    @Bean
    TransactionRunner transactionRunner(final PlatformTransactionManager transactionManager) {
        return new SpringTransactionRunner(transactionManager);
    }

    @Bean
    UnitOfWorkAspect unitOfWorkAspect(final TransactionRunner transactionRunner) {
        return new UnitOfWorkAspect(transactionRunner);
    }
}
