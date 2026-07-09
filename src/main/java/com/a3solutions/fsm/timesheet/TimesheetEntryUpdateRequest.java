package com.a3solutions.fsm.timesheet;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.timesheet
 * @project A3 Field Service Management Backend
 * @date 07/03/26
 */
@Schema(description = "Editable payroll fields for one timesheet entry. Only the owning technician can edit entries while the parent timesheet is DRAFT or REJECTED.")
public record TimesheetEntryUpdateRequest(
        @DecimalMin(value = "0.0", inclusive = true, message = "Miles cannot be negative.")
        @Digits(integer = 6, fraction = 2, message = "Miles must have at most two decimal places.")
        @Schema(description = "Mileage for payroll/reimbursement. Must be non-negative.", example = "40.00", nullable = true)
        BigDecimal miles,
        @Schema(description = "Onsite start time.", example = "08:00:00", nullable = true)
        LocalTime onsiteStartTime,
        @Schema(description = "Break start time. Must be supplied with breakEndTime.", example = "12:00:00", nullable = true)
        LocalTime breakStartTime,
        @Schema(description = "Break end time. Must be supplied with breakStartTime.", example = "12:30:00", nullable = true)
        LocalTime breakEndTime,
        @Schema(description = "Offsite end time. Cannot be before onsiteStartTime.", example = "16:00:00", nullable = true)
        LocalTime offsiteEndTime,
        @Size(max = 3000, message = "Comments cannot exceed 3000 characters.")
        @Schema(description = "Technician comments for payroll review.", example = "Completed service and obtained site sign-off.", maxLength = 3000, nullable = true)
        String comments
) {
}
