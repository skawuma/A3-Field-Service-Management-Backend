package com.a3solutions.fsm.auth;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.auth
 * @project A3 Field Service Management Backend
 * @date 11/17/25
 */
public record AuthResponse

        (        String accessToken,
                 String refreshToken,
                 String role
        )
{ }
