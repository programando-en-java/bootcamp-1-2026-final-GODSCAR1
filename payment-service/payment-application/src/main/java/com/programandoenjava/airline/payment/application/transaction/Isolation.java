package com.programandoenjava.airline.payment.application.transaction;

/**
 * How much the database has to pretend a unit of work is the only one running.
 *
 * <p>Declared here rather than reusing Spring's enum, for the reason ADR-009
 * gives: nothing in this module imports a framework.
 */
public enum Isolation {

    /** Whatever the database was configured with, which here is read committed. */
    DEFAULT,

    /**
     * The database must behave as if this ran alone. Asked for where a unit of
     * work reads a row to decide whether to write it, because read committed
     * lets two of them read the same absence and both write.
     *
     * <p>PostgreSQL enforces this by aborting one of the two with a
     * serialization failure rather than by making it wait, so anything asking
     * for it has to be safe to run again from the start.
     */
    SERIALIZABLE
}
