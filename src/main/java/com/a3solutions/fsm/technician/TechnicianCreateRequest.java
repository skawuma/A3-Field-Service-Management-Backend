package com.a3solutions.fsm.technician;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.technician
 * @project A3 Field Service Management Backend
 * @date 11/17/25
 */
@Schema(description = "Create or update payload for a technician profile.")
public record TechnicianCreateRequest (
        @Schema(description = "Technician first name.", example = "Deborah")
        @NotBlank String firstName,
        @Schema(description = "Technician last name.", example = "Katimbo")
        @NotBlank String lastName,
        @Schema(description = "Technician phone number.", example = "+1-312-555-0184")
        String phone,
        @Schema(description = "Technician email address.", example = "debs@a3fsm.com")
        String email,
        @Schema(description = "Comma-separated certification summary.", example = "HVAC, Generator Systems")
        String certifications,
        @Schema(description = "Technician availability status.", example = "ACTIVE")
        TechnicianStatus status

){ }
