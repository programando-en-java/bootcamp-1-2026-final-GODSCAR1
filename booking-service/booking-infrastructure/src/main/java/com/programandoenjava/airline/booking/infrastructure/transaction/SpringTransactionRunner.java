package com.programandoenjava.airline.booking.infrastructure.transaction;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

/** Two templates because readOnly is fixed on the template, not passed per call. */
public class SpringTransactionRunner implements TransactionRunner {

    private final TransactionTemplate readWriteTemplate;
    private final TransactionTemplate readOnlyTemplate;

    public SpringTransactionRunner(final PlatformTransactionManager transactionManager) {
        this.readWriteTemplate = new TransactionTemplate(transactionManager);
        this.readOnlyTemplate = new TransactionTemplate(transactionManager);
        this.readOnlyTemplate.setReadOnly(true);
    }

    @Override
    public <T> T executeInTransaction(final Supplier<T> action) {
        return readWriteTemplate.execute(status -> action.get());
    }

    @Override
    public <T> T executeReadOnly(final Supplier<T> action) {
        return readOnlyTemplate.execute(status -> action.get());
    }
}
