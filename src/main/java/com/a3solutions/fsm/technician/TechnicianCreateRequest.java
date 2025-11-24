package com.a3solutions.fsm.technician;

import jakarta.validation.constraints.NotBlank;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.technician
 * @project A3 Field Service Management Backend
 * @date 11/17/25
 */
public record TechnicianCreateRequest (
        @NotBlank String firstName,
        @NotBlank String lastName,
        String phone,
        String email,
        String certifications,
        TechnicianStatus status   // NEW: optional for update

){ }
