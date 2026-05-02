package com.a3solutions.fsm.auth;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.auth
 * @project A3 Field Service Management Backend
 * @date 11/17/25
 */
@Schema(description = "Refresh-token exchange request.")
public record RefreshTokenRequest(
        @Schema(description = "Previously issued refresh token.", example = "eyJhbGciOiJIUzI1NiJ9...")
        String refreshToken
) {
}
