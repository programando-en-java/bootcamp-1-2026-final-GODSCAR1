package com.programandoenjava.airline.flight.infrastructure.adapter.in.web;

import com.programandoenjava.airline.flight.domain.DomainValidationException;
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
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

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
    ProblemDetail onDomainValidation(DomainValidationException exception) {
        return badRequest("Invalid request", exception.getMessage());
    }

    /*
     * A sort field that is not in SortableField. Bean Validation checked the
     * shape of the expression; this is the whitelist.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail onIllegalArgument(IllegalArgumentException exception) {
        return badRequest("Invalid request parameter", exception.getMessage());
    }

    /*
     * Anything not handled above is a defect, not a bad request. The stack
     * trace is logged and deliberately not returned: internal details, table
     * names and class names have no business reaching a client.
     */
    @ExceptionHandler(Exception.class)
    ProblemDetail onUnexpectedError(Exception exception) {
        LOG.error("Unhandled exception", exception);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.");
        problem.setTitle("Internal server error");
        problem.setType(URI.create("urn:airline:problem:internal"));
        return problem;
    }

    private static ProblemDetail badRequest(String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setTitle(title);
        problem.setType(URI.create("urn:airline:problem:validation"));
        return problem;
    }
}