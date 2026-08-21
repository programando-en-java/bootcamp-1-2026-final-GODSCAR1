package com.programandoenjava.airline.booking.application.port.out.events;

public interface DomainEventPublisher {

    void publish(Object event);
}
