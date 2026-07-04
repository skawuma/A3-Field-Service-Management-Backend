package com.a3solutions.fsm.timesheet;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.timesheet
 * @project A3 Field Service Management Backend
 * @date 07/03/26
 */
public record TimesheetResponse(
        Long id,
        Long technicianId,
        String technicianName,
        LocalDate weekStartDate,
        LocalDate weekEndDate,
        TimesheetStatus status,
        Instant submittedAt,
        Instant approvedAt,
        String technicianSignatureText,
        Instant createdAt,
        Instant updatedAt,
        List<TimesheetEntryResponse> entries
) {
}
