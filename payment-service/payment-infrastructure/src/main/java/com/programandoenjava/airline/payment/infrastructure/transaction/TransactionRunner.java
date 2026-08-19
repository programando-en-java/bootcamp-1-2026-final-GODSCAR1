package com.programandoenjava.airline.payment.infrastructure.transaction;

import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

public interface TransactionRunner {

    <T> @Nullable T executeInTransaction(Supplier<T> action);

    <T> @Nullable T executeReadOnly(Supplier<T> action);
}
