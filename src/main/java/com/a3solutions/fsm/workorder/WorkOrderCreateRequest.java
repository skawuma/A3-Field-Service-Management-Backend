package com.a3solutions.fsm.workorder;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.workorder
 * @project A3 Field Service Management Backend
 * @date 11/17/25
 */
public record WorkOrderCreateRequest(
        @NotBlank String clientName,
        @NotBlank String address,
        String description,
        Long assignedTechId,
        LocalDate scheduledDate,
        String priority,
        WorkOrderStatus status
) {
}
