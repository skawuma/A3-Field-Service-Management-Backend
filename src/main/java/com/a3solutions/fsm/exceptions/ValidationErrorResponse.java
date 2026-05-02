package com.a3solutions.fsm.exceptions;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(
        name = "ValidationErrorResponse",
        description = "Validation failure payload with field-level errors."
)
public record ValidationErrorResponse(
        @Schema(description = "Timestamp when the error response was created.", example = "2026-05-02T14:30:00Z")
        String timestamp,
        @Schema(description = "HTTP status code.", example = "400")
        int status,
        @Schema(description = "Short error code or reason phrase.", example = "Bad Request")
        String error,
        @Schema(description = "Human-readable error message.", example = "Request validation failed.")
        String message,
        @Schema(description = "Request path that triggered the error.", example = "/api/technicians")
        String path,
        @Schema(
                description = "Map of invalid fields and their validation messages.",
                example = "{\"firstName\":\"must not be blank\",\"email\":\"must be a well-formed email address\"}"
        )
        Map<String, String> errors
) {
}
