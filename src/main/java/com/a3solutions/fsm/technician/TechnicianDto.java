package com.a3solutions.fsm.technician;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.technician
 * @project A3 Field Service Management Backend
 * @date 11/17/25
 */
@Schema(description = "Technician profile returned by technician endpoints.")
public record TechnicianDto(
        Long id,
        String firstName,
        String lastName,
        String phone,
        String email,
        String certifications,
        TechnicianStatus status


) { }
