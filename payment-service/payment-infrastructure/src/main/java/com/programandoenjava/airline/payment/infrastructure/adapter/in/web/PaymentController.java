package com.programandoenjava.airline.payment.infrastructure.adapter.in.web;

import com.programandoenjava.airline.payment.application.port.in.paybooking.PayBookingCommand;
import com.programandoenjava.airline.payment.application.port.in.paybooking.PayBookingUseCase;
import com.programandoenjava.airline.payment.domain.payment.Payment;
import com.programandoenjava.airline.payment.infrastructure.adapter.in.web.dto.PayBookingRequest;
import com.programandoenjava.airline.payment.infrastructure.adapter.in.web.dto.PaymentResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PayBookingUseCase payBookingUseCase;

    PaymentController(final PayBookingUseCase payBookingUseCase) {
        this.payBookingUseCase = payBookingUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    PaymentResponse pay(@Valid @RequestBody final PayBookingRequest request) {
        PayBookingCommand command = PayBookingRequestMapper.toCommand(request);

        Payment payment = payBookingUseCase.pay(command);

        return PaymentResponse.from(payment);
    }
}
