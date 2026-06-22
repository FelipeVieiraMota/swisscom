package com.swisscom.services.links.controller;

import com.swisscom.services.links.exception.InvalidLinkUrlException;
import com.swisscom.services.links.exception.LinkNotFoundException;
import com.swisscom.services.links.exception.ShortCodeGenerationException;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.validation.FieldError;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(LinkNotFoundException.class)
    ResponseEntity<@NonNull ApiError> handleNotFound(
            final LinkNotFoundException exception,
            final HttpServletRequest request
    ) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage(), request, List.of());
    }

    @ExceptionHandler({InvalidLinkUrlException.class, IllegalArgumentException.class})
    ResponseEntity<@NonNull ApiError> handleBadRequest(
            final RuntimeException exception,
            final HttpServletRequest request
    ) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage(), request, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<@NonNull ApiError> handleValidation(
            final MethodArgumentNotValidException exception,
            final HttpServletRequest request
    ) {
        final List<ApiFieldError> fields = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toFieldError)
                .toList();

        return error(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                request,
                fields
        );
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    ResponseEntity<@NonNull ApiError> handleForbidden(
            final AuthorizationDeniedException exception,
            final HttpServletRequest request
    ) {
        return error(
                HttpStatus.FORBIDDEN,
                "You do not have permission to access this resource",
                request,
                List.of()
        );
    }

    @ExceptionHandler(ShortCodeGenerationException.class)
    ResponseEntity<@NonNull ApiError> handleUnavailable(
            final ShortCodeGenerationException exception,
            final HttpServletRequest request
    ) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage(), request, List.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<@NonNull ApiError> handleUnexpected(
            final Exception exception,
            final HttpServletRequest request
    ) {
        return error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Unexpected internal server error",
                request,
                List.of()
        );
    }

    private ResponseEntity<@NonNull ApiError> error(
            final HttpStatus status,
            final String message,
            final HttpServletRequest request,
            final List<ApiFieldError> fields
    ) {
        final String requestId = request.getHeader("X-Request-Id");

        return ResponseEntity.status(status)
                .body(new ApiError(
                        Instant.now(),
                        status.value(),
                        status.getReasonPhrase(),
                        message,
                        request.getRequestURI(),
                        requestId,
                        fields
                ));
    }

    private ApiFieldError toFieldError(final FieldError fieldError) {
        return new ApiFieldError(
                fieldError.getField(),
                fieldError.getDefaultMessage()
        );
    }

    record ApiError(
            Instant timestamp,
            int status,
            String error,
            String message,
            String path,
            String requestId,
            List<ApiFieldError> fields
    ) {}

    record ApiFieldError(
            String field,
            String message
    ) {}
}