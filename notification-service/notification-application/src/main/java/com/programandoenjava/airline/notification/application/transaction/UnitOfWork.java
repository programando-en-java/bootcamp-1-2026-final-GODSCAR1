package com.programandoenjava.airline.notification.application.transaction;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
/* A serialisable unit of work is run again from the start on a conflict,
 * so it must not do anything it would be wrong to do twice. No network calls. */
public @interface UnitOfWork {

    boolean readOnly() default false;

    Isolation isolation() default Isolation.DEFAULT;
}
