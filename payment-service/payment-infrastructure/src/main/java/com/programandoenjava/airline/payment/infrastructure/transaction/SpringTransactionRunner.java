package com.programandoenjava.airline.payment.infrastructure.transaction;

import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

public class SpringTransactionRunner implements TransactionRunner {

    private static final int MAX_ATTEMPTS = 3;
    private static final long BACKOFF_MILLIS = 20;

    private final TransactionTemplate readWriteTemplate;
    private final TransactionTemplate serializableTemplate;
    private final TransactionTemplate readOnlyTemplate;

    public SpringTransactionRunner(final PlatformTransactionManager transactionManager) {
        this.readWriteTemplate = new TransactionTemplate(transactionManager);

        this.serializableTemplate = new TransactionTemplate(transactionManager);
        this.serializableTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);

        this.readOnlyTemplate = new TransactionTemplate(transactionManager);
        this.readOnlyTemplate.setReadOnly(true);
    }

    @Override
    public <T> T executeInTransaction(final Supplier<T> action) {
        return readWriteTemplate.execute(status -> action.get());
    }

    @Override
    /* Retrying is the other half of asking for serialisable: the whole action runs
     * again so its reads see what the winner left behind. */
    public <T> T executeSerializable(final Supplier<T> action) {
        for (int attempt = 1; attempt < MAX_ATTEMPTS; attempt++) {
            try {
                return runOnce(action);
            } catch (ConcurrencyFailureException conflict) {
                pauseAfter(attempt);
            }
        }

        return runOnce(action);
    }

    @Override
    public <T> T executeReadOnly(final Supplier<T> action) {
        return readOnlyTemplate.execute(status -> action.get());
    }

    private <T> T runOnce(final Supplier<T> action) {
        return serializableTemplate.execute(status -> action.get());
    }

    private static void pauseAfter(final int attempt) {
        try {
            Thread.sleep(BACKOFF_MILLIS * attempt);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting to retry", interrupted);
        }
    }
}
