package com.programandoenjava.airline.payment.infrastructure.config;

import com.programandoenjava.airline.payment.application.port.in.paybooking.PayBookingUseCase;
import com.programandoenjava.airline.payment.application.port.out.events.DomainEventPublisher;
import com.programandoenjava.airline.payment.application.port.out.gateway.PaymentGatewayPort;
import com.programandoenjava.airline.payment.application.port.out.readbooking.ReadBookingPort;
import com.programandoenjava.airline.payment.application.port.out.savepayment.SavePaymentPort;
import com.programandoenjava.airline.payment.application.usecase.PayBookingService;
import com.programandoenjava.airline.payment.application.usecase.PaymentRecorder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ApplicationConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    PaymentRecorder paymentRecorder(final SavePaymentPort savePaymentPort,
                                    final DomainEventPublisher domainEventPublisher) {
        return new PaymentRecorder(savePaymentPort, domainEventPublisher);
    }

    @Bean
    PayBookingUseCase payBookingUseCase(final ReadBookingPort readBookingPort,
                                        final PaymentGatewayPort paymentGatewayPort,
                                        final PaymentRecorder paymentRecorder,
                                        final Clock clock) {
        return new PayBookingService(readBookingPort, paymentGatewayPort,
                paymentRecorder, clock);
    }
}
