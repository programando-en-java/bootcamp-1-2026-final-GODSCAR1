package com.programandoenjava.airline.payment.application.port.out.events;

public interface DomainEventPublisher {

    void publish(Object event);
}
