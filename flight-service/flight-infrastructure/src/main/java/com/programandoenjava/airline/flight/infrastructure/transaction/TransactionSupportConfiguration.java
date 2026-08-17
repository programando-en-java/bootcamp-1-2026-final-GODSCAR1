package com.programandoenjava.airline.flight.infrastructure.transaction;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * {@code @EnableAspectJAutoProxy} is declared even though Boot's AspectJ
 * auto-configuration would normally do it. Hand-built slices run with
 * auto-configuration off, and without auto-proxying the aspect never fires. Here
 * that is not a silent inconvenience: the lock taken while blocking seats would
 * stop being held to commit, and the flight would oversell.
 *
 * <p>Any test building a use case with {@code new} rather than through the
 * context has the same gap, and nothing announces it.
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