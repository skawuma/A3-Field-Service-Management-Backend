package com.a3solutions.fsm.timesheet;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.timesheet
 * @project A3 Field Service Management Backend
 * @date 07/03/26
 */
public record TimesheetSubmitRequest(
        @NotBlank(message = "Technician signature is required.")
        @Size(max = 255, message = "Technician signature cannot exceed 255 characters.")
        String technicianSignatureText
) {
}
