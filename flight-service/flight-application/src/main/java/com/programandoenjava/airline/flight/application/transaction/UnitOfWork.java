package com.programandoenjava.airline.flight.application.transaction;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Runs the annotated use case inside one transaction. Declared here and
 * satisfied in infrastructure, which is what keeps this file free of any
 * framework import (ADR-009).
 *
 * <p>A method asking for {@link Isolation#SERIALIZABLE} is run again from the
 * start if the database refuses to serialise it, so it must not do anything it
 * would be wrong to do twice. In practice that means no network calls, which is
 * why every unit of work in this system is a class of its own with the calls
 * left outside it.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface UnitOfWork {

    boolean readOnly() default false;

    Isolation isolation() default Isolation.DEFAULT;
}
