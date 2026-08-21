package com.programandoenjava.airline.notification.infrastructure.transaction;

import java.util.function.Supplier;

public interface TransactionRunner {

    <T> T executeInTransaction(Supplier<T> action);

    /**
     * Runs the action in a serialisable transaction, starting it again if the
     * database refuses to serialise it.
     */
    <T> T executeSerializable(Supplier<T> action);

    <T> T executeReadOnly(Supplier<T> action);
}
