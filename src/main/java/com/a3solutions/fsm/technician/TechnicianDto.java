package com.a3solutions.fsm.technician;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.technician
 * @project A3 Field Service Management Backend
 * @date 11/17/25
 */
public record TechnicianDto(
        Long id,
        String firstName,
        String lastName,
        String phone,
        String email,
        String certifications,
        TechnicianStatus status


) { }
