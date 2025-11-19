package com.a3solutions.fsm.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.auth
 * @project A3 Field Service Management Backend
 * @date 11/17/25
 */
public record LoginRequest( @Email @NotBlank String email,
                            @NotBlank String password) {
}
