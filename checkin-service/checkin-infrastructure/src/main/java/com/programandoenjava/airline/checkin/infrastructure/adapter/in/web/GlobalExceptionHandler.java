package com.programandoenjava.airline.checkin.infrastructure.adapter.in.web;

import com.programandoenjava.airline.checkin.application.port.in.checkin.exception.BookingNotConfirmedException;
import com.programandoenjava.airline.checkin.application.port.out.readbooking.exception.BookingNotFoundException;
import com.programandoenjava.airline.checkin.application.port.out.readflight.exception.FlightNotFoundException;
import com.programandoenjava.airline.checkin.domain.checkin.exception.CheckInClosedException;
import com.programandoenjava.airline.checkin.domain.checkin.exception.CheckInNotOpenYetException;
import com.programandoenjava.airline.checkin.domain.checkin.exception.FlightDepartedException;
import com.programandoenjava.airline.checkin.domain.shared.DomainValidationException;
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
    private static final String UNAVAILABLE = "Check-in is unavailable";

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

    /* Also what a booking belonging to somebody else is answered with. */
    @ExceptionHandler(BookingNotFoundException.class)
    ProblemDetail handleBookingNotFound(final BookingNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "Booking not found",
                exception.getMessage(), "booking-not-found");
    }

    @ExceptionHandler(BookingNotConfirmedException.class)
    ProblemDetail handleBookingNotConfirmed(final BookingNotConfirmedException exception) {
        return problem(HttpStatus.CONFLICT, "Booking is not confirmed",
                exception.getMessage(), "booking-not-confirmed");
    }

    @ExceptionHandler(CheckInNotOpenYetException.class)
    ProblemDetail handleCheckInNotOpenYet(final CheckInNotOpenYetException exception) {
        return problem(HttpStatus.CONFLICT, "Check-in has not opened",
                exception.getMessage(), "check-in-not-open");
    }

    @ExceptionHandler(CheckInClosedException.class)
    ProblemDetail handleCheckInClosed(final CheckInClosedException exception) {
        return problem(HttpStatus.CONFLICT, "Check-in has closed",
                exception.getMessage(), "check-in-closed");
    }

    @ExceptionHandler(FlightDepartedException.class)
    ProblemDetail handleFlightDeparted(final FlightDepartedException exception) {
        return problem(HttpStatus.CONFLICT, "The flight has departed",
                exception.getMessage(), "flight-departed");
    }

    @ExceptionHandler(FlightNotFoundException.class)
    ProblemDetail handleFlightNotFound(final FlightNotFoundException exception) {
        logger.error("booking names a flight flight-service does not have", exception);

        return problem(HttpStatus.BAD_GATEWAY, UNAVAILABLE,
                "The flight for this booking could not be read.",
                "flight-not-found");
    }

    @ExceptionHandler(CallNotPermittedException.class)
    ProblemDetail handleCircuitOpen(final CallNotPermittedException exception) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, UNAVAILABLE,
                "Check-in cannot be done right now. Try again shortly.",
                "upstream-unavailable");
    }

    @ExceptionHandler(FeignException.class)
    ProblemDetail handleUpstreamFailure(final FeignException exception) {
        logger.error("an upstream call failed", exception);

        return problem(HttpStatus.BAD_GATEWAY, UNAVAILABLE,
                "The booking could not be read. Try again shortly.",
                "upstream-failed");
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
