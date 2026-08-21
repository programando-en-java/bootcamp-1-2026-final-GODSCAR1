package com.programandoenjava.airline.checkin.application.port.out.events;

public interface DomainEventPublisher {

    void publish(Object event);
}
