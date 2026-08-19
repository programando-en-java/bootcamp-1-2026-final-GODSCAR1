package com.programandoenjava.airline.payment.application.port.out.events;

/**
 * Announces something that happened, in process. A port rather than Spring's
 * publisher directly, so the use case stays free of the framework (ADR-006).
 */
public interface DomainEventPublisher {

    void publish(Object event);
}
