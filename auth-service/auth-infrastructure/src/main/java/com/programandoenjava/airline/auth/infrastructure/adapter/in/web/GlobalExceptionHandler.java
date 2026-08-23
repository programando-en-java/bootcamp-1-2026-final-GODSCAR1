package com.programandoenjava.airline.auth.infrastructure.adapter.in.web;

import com.programandoenjava.airline.auth.application.port.in.authenticate.exception.InvalidCredentialsException;
import com.programandoenjava.airline.auth.domain.shared.DomainValidationException;
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

    @ExceptionHandler(InvalidCredentialsException.class)
    ProblemDetail handleInvalidCredentials(final InvalidCredentialsException exception) {
        return problem(HttpStatus.UNAUTHORIZED, "Those credentials do not match an account",
                exception.getMessage(), "invalid-credentials");
    }

    @ExceptionHandler(DomainValidationException.class)
    ProblemDetail handleDomainValidation(final DomainValidationException exception) {
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
