package com.programandoenjava.airline.notification.infrastructure.transaction;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.transaction.PlatformTransactionManager;

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
