package com.a3solutions.fsm.exceptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "ApiErrorResponse",
        description = "Standard error payload returned when a request fails."
)
public record ApiErrorResponse(
        @Schema(description = "Timestamp when the error response was created.", example = "2026-05-02T14:30:00Z")
        String timestamp,
        @Schema(description = "HTTP status code.", example = "404")
        int status,
        @Schema(description = "Short error code or reason phrase.", example = "Not Found")
        String error,
        @Schema(description = "Human-readable error message.", example = "Work order not found: 42")
        String message,
        @Schema(description = "Request path that triggered the error.", example = "/api/workorders/42")
        String path
) {
}
