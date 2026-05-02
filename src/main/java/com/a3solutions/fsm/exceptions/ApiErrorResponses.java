package com.a3solutions.fsm.exceptions;

import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ApiErrorResponses {

    private ApiErrorResponses() {
    }

    public static ApiErrorResponse of(HttpStatus status, String message, String path) {
        return of(status, status.getReasonPhrase(), message, path);
    }

    public static ApiErrorResponse of(HttpStatus status, String error, String message, String path) {
        return new ApiErrorResponse(
                Instant.now().toString(),
                status.value(),
                error,
                message,
                path
        );
    }

    public static ValidationErrorResponse validation(HttpStatus status,
                                                     String message,
                                                     String path,
                                                     Map<String, String> errors) {
        return new ValidationErrorResponse(
                Instant.now().toString(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path,
                new LinkedHashMap<>(errors)
        );
    }
}
