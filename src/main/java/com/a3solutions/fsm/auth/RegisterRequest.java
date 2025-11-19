package com.a3solutions.fsm.auth;

import com.a3solutions.fsm.security.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.auth
 * @project A3 Field Service Management Backend
 * @date 11/17/25
 */
public record RegisterRequest(        @NotBlank String firstName,
                                      @NotBlank String lastName,
                                      @Email @NotBlank String email,
                                      @NotBlank String password,
                                      @NotNull Role role) {
}
