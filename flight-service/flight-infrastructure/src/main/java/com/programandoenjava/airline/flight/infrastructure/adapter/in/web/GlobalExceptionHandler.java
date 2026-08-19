package com.programandoenjava.airline.flight.infrastructure.adapter.in.web;

import com.programandoenjava.airline.flight.application.port.in.blockseats.exception.BookingAlreadyHoldsSeatsException;
import com.programandoenjava.airline.flight.application.port.shared.exception.FlightNotFoundException;
import com.programandoenjava.airline.flight.domain.flight.exception.FlightDepartedException;
import com.programandoenjava.airline.flight.domain.flight.exception.InsufficientSeatsException;
import com.programandoenjava.airline.flight.domain.shared.DomainValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /*
     * Bean Validation failures already come back as a 400 ProblemDetail from
     * the parent. This override adds which field failed and why, so the client
     * does not have to guess.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            final MethodArgumentNotValidException exception,
            final HttpHeaders headers,
            final HttpStatusCode status,
            final WebRequest request) {

        Map<String, String> errors = exception.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        fieldError -> fieldError.getField(),
                        fieldError -> fieldError.getDefaultMessage() == null
                                ? "is invalid"
                                : fieldError.getDefaultMessage(),
                        (first, second) -> first));

        ProblemDetail problem = badRequest("Invalid request parameter",
                "One or more parameters are invalid");
        problem.setProperty("errors", errors);

        return ResponseEntity.badRequest().body(problem);
    }

    /*
     * A domain invariant violated by client input: an airport code that is not
     * three letters, a currency that does not exist.
     */
    @ExceptionHandler(DomainValidationException.class)
    ProblemDetail onDomainValidation(final DomainValidationException exception) {
        return badRequest("Invalid request", exception.getMessage());
    }

    /*
     * A sort field that is not in SortableField. Bean Validation checked the
     * shape of the expression; this is the whitelist.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail onIllegalArgument(final IllegalArgumentException exception) {
        return badRequest("Invalid request parameter", exception.getMessage());
    }

    /*
     * Anything not handled above is a defect, not a bad request. The stack
     * trace is logged and deliberately not returned: internal details, table
     * names and class names have no business reaching a client.
     */
    @ExceptionHandler(Exception.class)
    ProblemDetail onUnexpectedError(final Exception exception) {
        LOG.error("Unhandled exception", exception);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.");
        problem.setTitle("Internal server error");
        problem.setType(URI.create("urn:airline:problem:internal"));
        return problem;
    }

    /**
     * The flight exists but has gone. Nothing the caller changes about the
     * request will make it succeed, which is what separates this from a
     * conflict: 409 invites a retry, 422 does not.
     */
    @ExceptionHandler(FlightDepartedException.class)
    ProblemDetail handleFlightDeparted(final FlightDepartedException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_CONTENT, exception.getMessage());
        problem.setTitle("Flight is no longer bookable");
        problem.setType(URI.create("urn:airline:problem:flight-departed"));
        return problem;
    }

    /**
     * The request was well formed and may succeed later, or for a smaller party.
     * The message carries how many seats were left, which is the only thing that
     * makes this answer actionable.
     */
    @ExceptionHandler(InsufficientSeatsException.class)
    ProblemDetail handleInsufficientSeats(final InsufficientSeatsException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Not enough seats");
        problem.setType(URI.create("urn:airline:problem:insufficient-seats"));
        return problem;
    }

    /**
     * A booking asked for seats twice under two different keys. Refused rather
     * than honoured: a booking holding two sets of seats is a booking holding
     * seats nobody will pay for.
     */
    @ExceptionHandler(BookingAlreadyHoldsSeatsException.class)
    ProblemDetail handleBookingAlreadyHoldsSeats(final BookingAlreadyHoldsSeatsException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Booking already holds seats");
        problem.setType(URI.create("urn:airline:problem:booking-already-held"));
        return problem;
    }

    @ExceptionHandler(FlightNotFoundException.class)
    ProblemDetail handleFlightNotFound(final FlightNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Flight not found");
        problem.setType(URI.create("urn:airline:problem:flight-not-found"));
        return problem;
    }

    private static ProblemDetail badRequest(final String title, final String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setTitle(title);
        problem.setType(URI.create("urn:airline:problem:validation"));
        return problem;
    }
}