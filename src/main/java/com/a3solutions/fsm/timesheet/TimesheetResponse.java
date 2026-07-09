package com.a3solutions.fsm.timesheet;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.timesheet
 * @project A3 Field Service Management Backend
 * @date 07/03/26
 */
@Schema(description = "Weekly technician timesheet returned to technician, dispatcher, and payroll review screens.")
public record TimesheetResponse(
        @Schema(description = "Timesheet identifier.", example = "12")
        Long id,
        @Schema(description = "Technician profile identifier that owns this timesheet.", example = "7")
        Long technicianId,
        @Schema(description = "Technician display name captured for payroll review.", example = "James Carter")
        String technicianName,
        @Schema(description = "Monday week-start date for this timesheet.", example = "2026-06-29")
        LocalDate weekStartDate,
        @Schema(description = "Sunday week-end date for this timesheet.", example = "2026-07-05")
        LocalDate weekEndDate,
        @Schema(description = "Current payroll review status.", example = "DRAFT")
        TimesheetStatus status,
        @Schema(description = "Timestamp when the technician submitted the timesheet.", example = "2026-07-03T20:10:00Z", nullable = true)
        Instant submittedAt,
        @Schema(description = "Timestamp when ADMIN or DISPATCH approved the timesheet.", example = "2026-07-03T21:15:00Z", nullable = true)
        Instant approvedAt,
        @Schema(description = "Typed technician signature captured at submission time.", example = "James Carter", nullable = true)
        String technicianSignatureText,
        @Schema(description = "Timestamp when the timesheet was created.", example = "2026-07-01T13:00:00Z")
        Instant createdAt,
        @Schema(description = "Timestamp when the timesheet was last updated.", example = "2026-07-03T20:10:00Z")
        Instant updatedAt,
        @ArraySchema(schema = @Schema(implementation = TimesheetEntryResponse.class))
        List<TimesheetEntryResponse> entries
) {
}
