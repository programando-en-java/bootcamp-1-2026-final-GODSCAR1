package com.programandoenjava.airline.flight.infrastructure.transaction;

import com.programandoenjava.airline.flight.application.transaction.UnitOfWork;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.function.Supplier;

@Aspect
public class UnitOfWorkAspect {

    private final TransactionRunner transactionRunner;

    public UnitOfWorkAspect(final TransactionRunner transactionRunner) {
        this.transactionRunner = transactionRunner;
    }

    @Around("@annotation(unitOfWork)")
    public @Nullable Object runInUnitOfWork(final ProceedingJoinPoint joinPoint, final UnitOfWork unitOfWork) {
        Supplier<Object> action = () -> proceed(joinPoint);
        return unitOfWork.readOnly()
                ? transactionRunner.executeReadOnly(action)
                : transactionRunner.executeInTransaction(action);
    }

    /*
     * proceed() is declared to throw Throwable and Supplier.get() cannot. The
     * checked branch is dead while no use case declares a checked exception, but
     * it has to compile: UndeclaredThrowableException is what the JDK's own
     * proxies use for exactly this, so a stack trace reads the same way.
     */
    private static Object proceed(final ProceedingJoinPoint joinPoint) {
        try {
            return joinPoint.proceed();
        } catch (final RuntimeException | Error unchecked) {
            throw unchecked;
        } catch (final Throwable checked) {
            throw new UndeclaredThrowableException(checked);
        }
    }
}
