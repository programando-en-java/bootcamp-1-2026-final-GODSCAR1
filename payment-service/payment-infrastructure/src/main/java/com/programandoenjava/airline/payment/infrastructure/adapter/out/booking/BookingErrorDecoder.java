package com.programandoenjava.airline.payment.infrastructure.adapter.out.booking;

import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.http.HttpStatus;

/*
 * Compared by number, not by constant: Spring 7 renamed UNPROCESSABLE_ENTITY,
 * and a decoder naming a constant that moved answers 502 for a refusal the
 * caller could have understood. Numbers do not get renamed.
 *
 * A 404 is left to the caller to raise, because only it knows which booking it
 * asked about.
 */
class BookingErrorDecoder implements ErrorDecoder {

    private static final ErrorDecoder DEFAULT = new ErrorDecoder.Default();

    static final int NOT_FOUND = HttpStatus.NOT_FOUND.value();

    @Override
    public Exception decode(final String methodKey, final Response response) {
        return DEFAULT.decode(methodKey, response);
    }
}
