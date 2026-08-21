package com.programandoenjava.airline.payment.infrastructure.adapter.out.booking;

import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.http.HttpStatus;

class BookingErrorDecoder implements ErrorDecoder {

    private static final ErrorDecoder DEFAULT = new ErrorDecoder.Default();

    static final int NOT_FOUND = HttpStatus.NOT_FOUND.value();

    @Override
    public Exception decode(final String methodKey, final Response response) {
        return DEFAULT.decode(methodKey, response);
    }
}
