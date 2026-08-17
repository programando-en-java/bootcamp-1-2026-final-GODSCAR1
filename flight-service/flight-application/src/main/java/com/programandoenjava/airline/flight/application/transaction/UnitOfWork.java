package com.programandoenjava.airline.flight.application.transaction;

import java.lang.annotation.*;

/**
 * Runs the annotated use case inside one transaction.
 *
 * <p>Declared here, in the application layer, and satisfied in infrastructure by
 * an aspect delegating to {@code TransactionRunner}. The annotation states a
 * need; the runner meets it. That is the port and adapter relationship, written
 * as an annotation instead of an interface, and it is why this file imports
 * nothing from any framework.
 *
 * <p>RUNTIME retention is mandatory. With CLASS the annotation never reaches the
 * aspect, every annotated method runs outside a transaction, and nothing says so.
 *
 * <p>Being proxy-based, it only takes effect on calls arriving from outside the
 * bean. Annotate the method implementing the inbound port, never one the service
 * calls on itself.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface UnitOfWork {

    boolean readOnly() default false;
}
