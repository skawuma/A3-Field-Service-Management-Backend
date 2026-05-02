package com.a3solutions.fsm.auth;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.auth
 * @project A3 Field Service Management Backend
 * @date 11/17/25
 */
@Schema(description = "Authentication response containing JWT tokens and the authenticated role.")
public record AuthResponse(
        @Schema(description = "JWT access token used for authenticated API requests.", example = "eyJhbGciOiJIUzI1NiJ9...")
        String accessToken,
        @Schema(description = "JWT refresh token used to obtain a new access token.", example = "eyJhbGciOiJIUzI1NiJ9...")
        String refreshToken,
        @Schema(description = "Authenticated user role.", example = "ADMIN")
        String role
) {
}
