package com.programandoenjava.airline.flight.infrastructure.transaction;

import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

/** Three templates because readOnly and isolation are fixed on the template, not passed per call. */
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

    /*
     * A serialization failure is how PostgreSQL says "you two collided, one of
     * you go again", not that anything is broken. Retrying is the other half of
     * asking for serialisable, and without it a second click would be answered
     * with a 500 instead of the booking the first one made.
     *
     * The whole action runs again, because the point is that its reads see the
     * world the winner left behind. That is only safe because a unit of work
     * here touches one database and nothing else.
     */
    @Override
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

    /* Backing off a little keeps two callers from colliding again immediately. */
    private static void pauseAfter(final int attempt) {
        try {
            Thread.sleep(BACKOFF_MILLIS * attempt);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting to retry", interrupted);
        }
    }
}
