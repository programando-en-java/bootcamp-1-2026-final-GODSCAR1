package com.programandoenjava.airline.payment.application.transaction;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Runs the annotated use case inside one transaction. Declared here and
 * satisfied in infrastructure, which is what keeps this file free of any
 * framework import (ADR-009).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface UnitOfWork {

    boolean readOnly() default false;
}
