package com.a3solutions.fsm.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.auth
 * @project A3 Field Service Management Backend
 * @date 11/17/25
 */
@RestController
@RequestMapping("/api/auth")
@Tag(
        name = "Authentication",
        description = "Authentication endpoints for login, refresh-token exchange, and tightly controlled development bootstrap flows."
)
@SecurityRequirements
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(
            summary = "Bootstrap a default admin account",
            description = "Creates a bootstrap admin account using configured bootstrap credentials. This endpoint is intended for development/bootstrap use only and should remain disabled outside explicitly allowed environments."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Admin account created", content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "400", description = "Admin account could not be created"),
            @ApiResponse(responseCode = "404", description = "Admin bootstrap is disabled")
    })
    @PostMapping("/create-admin")
    public ResponseEntity<String> createAdmin() {
        return authService.createAdmin();
    }

    @Operation(
            summary = "Register a new user",
            description = "Creates a new self-registered technician account and immediately returns access and refresh tokens. This endpoint is disabled by default for production hardening and rejects elevated roles."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User registered successfully", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Registration request is invalid, email already exists, or the requested role is not allowed"),
            @ApiResponse(responseCode = "404", description = "Self-registration is disabled")
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @Operation(
            summary = "Authenticate a user",
            description = "Validates user credentials and returns a JWT access token plus a refresh token."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Email or password is invalid")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(
            summary = "Refresh an authenticated session",
            description = "Exchanges a valid refresh token for a new access token and refresh token pair."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token refresh successful", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Refresh token is invalid or expired")
    })
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }
}
