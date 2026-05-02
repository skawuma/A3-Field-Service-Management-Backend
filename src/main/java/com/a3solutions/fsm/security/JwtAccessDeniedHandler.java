package com.a3solutions.fsm.security;

import com.a3solutions.fsm.exceptions.ApiErrorResponses;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.security
 * @project A3 Field Service Management Backend
 * @date 11/21/25
 */

@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");

        mapper.writeValue(
                response.getOutputStream(),
                ApiErrorResponses.of(
                        HttpStatus.FORBIDDEN,
                        "FORBIDDEN",
                        accessDeniedException.getMessage(),
                        request.getRequestURI()
                )
        );

    }
}
