package com.a3solutions.fsm.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.auth
 * @project A3 Field Service Management Backend
 * @date 11/17/25
 */
@Schema(description = "Credentials used to authenticate and receive JWT tokens.")
public record LoginRequest(
        @Schema(description = "User email address.", example = "admin@a3fsm.com")
        @Email @NotBlank String email,
        @Schema(description = "User password.", example = "admin123")
        @NotBlank String password
) {
}
