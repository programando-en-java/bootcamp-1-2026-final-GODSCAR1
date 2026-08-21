package com.programandoenjava.airline.flight.infrastructure.transaction;

import com.programandoenjava.airline.flight.application.transaction.Isolation;
import com.programandoenjava.airline.flight.application.transaction.UnitOfWork;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.function.Supplier;

@Aspect
public class UnitOfWorkAspect {

    private final TransactionRunner transactionRunner;

    public UnitOfWorkAspect(final TransactionRunner transactionRunner) {
        this.transactionRunner = transactionRunner;
    }

    @Around("@annotation(unitOfWork)")
    public Object runInUnitOfWork(final ProceedingJoinPoint joinPoint,
                                  final UnitOfWork unitOfWork) {
        Supplier<Object> action = () -> proceed(joinPoint);

        boolean readOnly = unitOfWork.readOnly();
        Isolation isolation = unitOfWork.isolation();

        if (readOnly) {
            return transactionRunner.executeReadOnly(action);
        }
        if (isolation == Isolation.SERIALIZABLE) {
            return transactionRunner.executeSerializable(action);
        }

        return transactionRunner.executeInTransaction(action);
    }

    private static Object proceed(final ProceedingJoinPoint joinPoint) {
        try {
            return joinPoint.proceed();
        } catch (RuntimeException | Error unchecked) {
            throw unchecked;
        } catch (Throwable checked) {
            throw new UndeclaredThrowableException(checked);
        }
    }
}
