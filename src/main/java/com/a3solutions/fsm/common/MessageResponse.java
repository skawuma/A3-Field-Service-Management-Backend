package com.a3solutions.fsm.common;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Simple response wrapper used for success messages.")
public record MessageResponse(
        @Schema(description = "Human-readable result message.", example = "Attachment deleted successfully")
        String message
) {
}
