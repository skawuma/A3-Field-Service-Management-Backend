package com.a3solutions.fsm.timesheet;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.timesheet
 * @project A3 Field Service Management Backend
 * @date 07/03/26
 */
@Schema(
        description = "Timesheet lifecycle status. DRAFT and REJECTED are editable by the owning technician; SUBMITTED is reviewable; APPROVED is locked for payroll."
)
public enum TimesheetStatus {
    DRAFT,
    SUBMITTED,
    APPROVED,
    REJECTED,
    EXPORTED
}
