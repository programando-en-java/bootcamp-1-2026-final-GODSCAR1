package com.programandoenjava.airline.booking.infrastructure.adapter.in.events;

import com.programandoenjava.airline.booking.application.port.in.settlebooking.ConfirmBookingUseCase;
import com.programandoenjava.airline.booking.application.port.in.settlebooking.FailBookingUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class PaymentEventsConfiguration {

    @Bean
    PaymentEventsListener paymentEventsListener(final ConfirmBookingUseCase confirmBookingUseCase,
                                                final FailBookingUseCase failBookingUseCase,
                                                final ObjectMapper objectMapper) {
        return new PaymentEventsListener(confirmBookingUseCase, failBookingUseCase, objectMapper);
    }
}
