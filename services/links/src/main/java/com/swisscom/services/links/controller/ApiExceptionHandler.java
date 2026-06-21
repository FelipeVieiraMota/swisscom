package com.swisscom.services.links.controller;

import com.swisscom.services.links.exception.InvalidLinkUrlException;
import com.swisscom.services.links.exception.LinkNotFoundException;
import com.swisscom.services.links.exception.ShortCodeGenerationException;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(LinkNotFoundException.class)
    ResponseEntity<@NonNull ApiError> handleNotFound(final LinkNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler({InvalidLinkUrlException.class, IllegalArgumentException.class})
    ResponseEntity<@NonNull ApiError> handleBadRequest(final RuntimeException exception) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<@NonNull ApiError> handleValidation(final MethodArgumentNotValidException exception) {
        final String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(field -> field.getField() + ": " + field.getDefaultMessage())
                .orElse("Invalid request");
        return error(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(ShortCodeGenerationException.class)
    ResponseEntity<@NonNull ApiError> handleUnavailable(final ShortCodeGenerationException exception) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage());
    }

    private ResponseEntity<@NonNull ApiError> error(final HttpStatus status, final String message) {
        return ResponseEntity.status(status)
                .body(new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message));
    }

    record ApiError(Instant timestamp, int status, String error, String message) {}
}
