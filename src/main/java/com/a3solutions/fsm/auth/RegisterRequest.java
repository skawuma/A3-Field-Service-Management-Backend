package com.a3solutions.fsm.auth;

import com.a3solutions.fsm.security.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.auth
 * @project A3 Field Service Management Backend
 * @date 11/17/25
 */
@Schema(description = "Request payload for registering a new user.")
public record RegisterRequest(
        @Schema(description = "User first name.", example = "Deborah")
        @NotBlank String firstName,
        @Schema(description = "User last name.", example = "Katimbo")
        @NotBlank String lastName,
        @Schema(description = "Unique email address.", example = "debs@a3fsm.com")
        @Email @NotBlank String email,
        @Schema(description = "User password.", example = "debs123")
        @NotBlank String password,
        @Schema(description = "Role assigned to the new user.", example = "TECH")
        @NotNull Role role
) {
}
