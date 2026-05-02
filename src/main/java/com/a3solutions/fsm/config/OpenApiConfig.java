package com.a3solutions.fsm.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Paste an access token returned by /api/auth/login."
)
public class OpenApiConfig {

    @Bean
    public OpenAPI a3FsmOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("A3 Field Service Management API")
                        .version("Sprint 8")
                        .description("""
                                REST API contract for A3 FSM.

                                Main workflow:
                                OPEN -> ASSIGNED -> IN_PROGRESS -> COMPLETED -> REOPENED/OPEN

                                Authenticated endpoints use JWT bearer tokens.
                                Realtime dashboard updates are delivered separately over WebSocket/STOMP.
                                """)
                        .license(new License()
                                .name("Internal project use")));
    }
}
