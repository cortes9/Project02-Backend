package com.studygroupfinder.api.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.net.URI;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final String PROBLEM_TYPE_PREFIX =
            "urn:study-group-finder:problem:";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {

        String detail = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining("; "));

        if (detail.isBlank()) {
            detail = "Request validation failed.";
        }

        return problem(
                HttpStatus.BAD_REQUEST,
                "validation-error",
                detail,
                request
        );
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            ConstraintViolationException.class
    })
    ResponseEntity<ProblemDetail> handleMalformedRequest(
            Exception exception,
            HttpServletRequest request) {

        return problem(
                HttpStatus.BAD_REQUEST,
                "invalid-request",
                "The request is invalid or malformed.",
                request
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ProblemDetail> handleIllegalArgument(
            IllegalArgumentException exception,
            HttpServletRequest request) {

        return problem(
                HttpStatus.BAD_REQUEST,
                "invalid-request",
                safeDetail(exception.getMessage(), "The request is invalid."),
                request
        );
    }

    @ExceptionHandler(AuthenticationRequiredException.class)
    ResponseEntity<ProblemDetail> handleAuthenticationRequired(
            AuthenticationRequiredException exception,
            HttpServletRequest request) {

        return problem(
                HttpStatus.UNAUTHORIZED,
                "unauthorized",
                safeDetail(exception.getMessage(), "Authentication is required."),
                request
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ProblemDetail> handleAccessDenied(
            AccessDeniedException exception,
            HttpServletRequest request) {

        return problem(
                HttpStatus.FORBIDDEN,
                "forbidden",
                "You do not have permission to access this resource.",
                request
        );
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ProblemDetail> handleNotFound(
            ResourceNotFoundException exception,
            HttpServletRequest request) {

        return problem(
                HttpStatus.NOT_FOUND,
                "resource-not-found",
                safeDetail(exception.getMessage(), "The requested resource was not found."),
                request
        );
    }

    @ExceptionHandler(ConflictException.class)
    ResponseEntity<ProblemDetail> handleConflict(
            ConflictException exception,
            HttpServletRequest request) {

        return problem(
                HttpStatus.CONFLICT,
                "conflict",
                safeDetail(exception.getMessage(), "The request conflicts with the current resource state."),
                request
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ProblemDetail> handleDataConflict(
            DataIntegrityViolationException exception,
            HttpServletRequest request) {

        return problem(
                HttpStatus.CONFLICT,
                "conflict",
                "The request conflicts with the current resource state.",
                request
        );
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ProblemDetail> handleResponseStatus(
            ResponseStatusException exception,
            HttpServletRequest request) {

        HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value());
        if (status == null || status.is5xxServerError()) {
            return internalServerError(request);
        }

        return problem(
                status,
                status.name().toLowerCase().replace('_', '-'),
                safeDetail(exception.getReason(), status.getReasonPhrase()),
                request
        );
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpected(
            Exception exception,
            HttpServletRequest request) {

        return internalServerError(request);
    }

    private ResponseEntity<ProblemDetail> internalServerError(
            HttpServletRequest request) {

        return problem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "internal-server-error",
                "An unexpected error occurred.",
                request
        );
    }

    private ResponseEntity<ProblemDetail> problem(
            HttpStatus status,
            String problemType,
            String detail,
            HttpServletRequest request) {

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                status,
                detail
        );
        problemDetail.setType(URI.create(PROBLEM_TYPE_PREFIX + problemType));
        problemDetail.setTitle(status.getReasonPhrase());
        problemDetail.setInstance(URI.create(request.getRequestURI()));

        return ResponseEntity
                .status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problemDetail);
    }

    private String safeDetail(String detail, String fallback) {
        return detail == null || detail.isBlank() ? fallback : detail;
    }
}
