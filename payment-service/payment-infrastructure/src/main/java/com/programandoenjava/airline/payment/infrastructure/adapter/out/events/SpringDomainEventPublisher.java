package com.programandoenjava.airline.payment.infrastructure.adapter.out.events;

import com.programandoenjava.airline.payment.application.port.out.events.DomainEventPublisher;
import org.springframework.context.ApplicationEventPublisher;

class SpringDomainEventPublisher implements DomainEventPublisher {

    private final ApplicationEventPublisher publisher;

    SpringDomainEventPublisher(final ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publish(final Object event) {
        publisher.publishEvent(event);
    }
}
