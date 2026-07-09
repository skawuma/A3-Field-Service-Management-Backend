package com.a3solutions.fsm.timesheet;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.timesheet
 * @project A3 Field Service Management Backend
 * @date 07/03/26
 */
@Schema(description = "Technician signature payload used to submit a weekly timesheet for ADMIN or DISPATCH payroll review.")
public record TimesheetSubmitRequest(
        @NotBlank(message = "Technician signature is required.")
        @Size(max = 255, message = "Technician signature cannot exceed 255 characters.")
        @Schema(description = "Typed technician signature. This is never written to application logs.", example = "James Carter", maxLength = 255)
        String technicianSignatureText
) {
}
