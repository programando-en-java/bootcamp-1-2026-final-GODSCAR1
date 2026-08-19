package com.programandoenjava.airline.payment.infrastructure.adapter.out.gateway;

import com.programandoenjava.airline.payment.application.port.out.gateway.PaymentGatewayPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfiguration {

    @Bean
    PaymentGatewayPort paymentGatewayPort() {
        return new DecliningCardGateway();
    }
}
