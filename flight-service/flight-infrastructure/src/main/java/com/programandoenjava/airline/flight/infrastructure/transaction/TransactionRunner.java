package com.programandoenjava.airline.flight.infrastructure.transaction;

import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Runs an action inside a transaction.
 *
 * <p>Lives in infrastructure rather than the application layer because nothing
 * in the application layer calls it. The application declares its need with
 * {@code @UnitOfWork}; this is the machinery that answers, and keeping the two
 * apart is what lets the annotation stay free of any framework import.
 *
 * <p>The return is nullable to match what it wraps: Spring's TransactionTemplate
 * hands back whatever the action returned, null included. No use case here
 * returns null, but the type says what is possible rather than what happens to
 * be true today.
 */
public interface TransactionRunner {

    <T> @Nullable T executeInTransaction(Supplier<T> action);

    <T> @Nullable T executeReadOnly(Supplier<T> action);
}
