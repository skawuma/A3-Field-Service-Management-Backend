package com.a3solutions.fsm.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.security
 * @project A3 Field Service Management Backend
 * @date 11/21/25
 */

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper mapper = new ObjectMapper();


    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");

        Map<String, Object> body = Map.of(
                "status", 401,
                "error", "UNAUTHORIZED",
                "message", authException.getMessage(),
                "path", request.getRequestURI()
        );

        mapper.writeValue(response.getOutputStream(), body);

    }
}
