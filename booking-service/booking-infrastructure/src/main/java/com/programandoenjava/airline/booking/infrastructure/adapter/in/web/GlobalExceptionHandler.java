package com.programandoenjava.airline.booking.infrastructure.adapter.in.web;

import com.programandoenjava.airline.booking.application.port.out.holdseats.exception.FlightNotBookableException;
import com.programandoenjava.airline.booking.application.port.out.holdseats.exception.FlightNotFoundException;
import com.programandoenjava.airline.booking.application.port.out.holdseats.exception.SeatsUnavailableException;
import com.programandoenjava.airline.booking.domain.shared.DomainValidationException;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String PROBLEM_BASE = "urn:airline:problem:";
    private static final String INVALID_PARAMETER = "Invalid request parameter";
    private static final String UNAVAILABLE = "Bookings are unavailable";

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            final MethodArgumentNotValidException exception,
            final HttpHeaders headers,
            final HttpStatusCode status,
            final WebRequest request) {

        Map<String, String> errors = exception.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        error -> error.getField(),
                        error -> String.valueOf(error.getDefaultMessage()),
                        (first, second) -> first));

        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, INVALID_PARAMETER,
                "One or more parameters are invalid", "validation");
        problem.setProperty("errors", errors);

        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(SeatsUnavailableException.class)
    ProblemDetail handleSeatsUnavailable(final SeatsUnavailableException exception) {
        return problem(HttpStatus.CONFLICT, "Not enough seats",
                exception.getMessage(), "seats-unavailable");
    }

    @ExceptionHandler(FlightNotBookableException.class)
    ProblemDetail handleFlightNotBookable(final FlightNotBookableException exception) {
        return problem(HttpStatus.UNPROCESSABLE_CONTENT, "Flight is no longer bookable",
                exception.getMessage(), "flight-not-bookable");
    }

    @ExceptionHandler(FlightNotFoundException.class)
    ProblemDetail handleFlightNotFound(final FlightNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "Flight not found",
                exception.getMessage(), "flight-not-found");
    }

    @ExceptionHandler(CallNotPermittedException.class)
    ProblemDetail handleCircuitOpen(final CallNotPermittedException exception) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, UNAVAILABLE,
                "Seats cannot be held right now. Try again shortly.",
                "flight-service-unavailable");
    }

    @ExceptionHandler(FeignException.class)
    ProblemDetail handleFlightServiceFailure(final FeignException exception) {
        logger.error("flight-service call failed", exception);

        return problem(HttpStatus.BAD_GATEWAY, UNAVAILABLE,
                "Seats could not be held. Try again shortly.", "flight-service-failed");
    }

    @ExceptionHandler(DomainValidationException.class)
    ProblemDetail handleDomainValidation(final DomainValidationException exception) {
        return problem(HttpStatus.BAD_REQUEST, INVALID_PARAMETER,
                exception.getMessage(), "validation");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleIllegalArgument(final IllegalArgumentException exception) {
        return problem(HttpStatus.BAD_REQUEST, INVALID_PARAMETER,
                exception.getMessage(), "validation");
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(final Exception exception) {
        logger.error("Unhandled exception", exception);

        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong",
                "The request could not be completed", "internal");
    }

    private static ProblemDetail problem(final HttpStatus status,
                                         final String title,
                                         final String detail,
                                         final String type) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create(PROBLEM_BASE + type));

        return problem;
    }
}
