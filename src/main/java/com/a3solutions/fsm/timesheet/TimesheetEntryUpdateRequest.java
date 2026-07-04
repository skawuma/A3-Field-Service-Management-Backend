package com.a3solutions.fsm.timesheet;

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
public record TimesheetEntryUpdateRequest(
        @DecimalMin(value = "0.0", inclusive = true, message = "Miles cannot be negative.")
        @Digits(integer = 6, fraction = 2, message = "Miles must have at most two decimal places.")
        BigDecimal miles,
        LocalTime onsiteStartTime,
        LocalTime breakStartTime,
        LocalTime breakEndTime,
        LocalTime offsiteEndTime,
        @Size(max = 3000, message = "Comments cannot exceed 3000 characters.")
        String comments
) {
}
