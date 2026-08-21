package com.programandoenjava.airline.booking.infrastructure.adapter.out.flight;

import com.programandoenjava.airline.booking.application.port.out.holdseats.exception.FlightNotBookableException;
import com.programandoenjava.airline.booking.application.port.out.holdseats.exception.FlightNotFoundException;
import com.programandoenjava.airline.booking.application.port.out.holdseats.exception.SeatsUnavailableException;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.http.HttpStatus;

class FlightErrorDecoder implements ErrorDecoder {

    private static final ErrorDecoder DEFAULT = new ErrorDecoder.Default();

    @Override
    public Exception decode(final String methodKey, final Response response) {
        int status = response.status();

        if (status == HttpStatus.CONFLICT.value()) {
            return new SeatsUnavailableException("The flight has no seats left to hold");
        }
        if (status == HttpStatus.UNPROCESSABLE_CONTENT.value()) {
            return new FlightNotBookableException("The flight is no longer open for booking");
        }
        if (status == HttpStatus.NOT_FOUND.value()) {
            return new FlightNotFoundException("No such flight");
        }
        return DEFAULT.decode(methodKey, response);
    }
}
